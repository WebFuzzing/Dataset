param(
  [string]$Region   = "us-west-2",
  [string]$Endpoint = "http://localhost:4566"
)

$ErrorActionPreference = "Stop"

function awsLocal {
  param([Parameter(ValueFromRemainingArguments = $true)]$Args)
  aws --endpoint-url $Endpoint --region $Region @Args
}

Write-Host "Using AWS endpoint $Endpoint (region $Region)"

# 1) Wait for DynamoDB to be ready (probe DDB directly; no STS needed)
Write-Host "Waiting for DynamoDB to be ready..."
$max = 40
for ($i=0; $i -lt $max; $i++) {
  try {
    awsLocal dynamodb list-tables 1>$null 2>$null
    break
  } catch {
    Start-Sleep -Seconds 1
    if ($i -eq ($max - 1)) { throw "DynamoDB not responding on $Endpoint" }
  }
}

# 2) Helpers
function Test-TableExists {
  param([string]$TableName)
  $name = awsLocal dynamodb list-tables `
    --query "TableNames[?@=='$TableName'] | [0]" `
    --output text 2>$null
  return ($name -eq $TableName)
}

function Ensure-TableJson {
  param(
    [string]$TableName,
    [array]$AttributeDefinitions, # @(@{AttributeName="..."; AttributeType="S"}, ...)
    [array]$KeySchema,            # @(@{AttributeName="..."; KeyType="HASH"}, @{...; KeyType="RANGE"})
    [array]$GlobalSecondaryIndexes # optional: array of GSI specs (or $null)
  )

  if (Test-TableExists $TableName) {
    Write-Host "$TableName already exists"
    return
  }

  Write-Host "Creating $TableName..."

  $req = [ordered]@{
    TableName            = $TableName
    BillingMode          = "PAY_PER_REQUEST"
    AttributeDefinitions = $AttributeDefinitions
    KeySchema            = $KeySchema
  }

  if ($GlobalSecondaryIndexes -and $GlobalSecondaryIndexes.Count -gt 0) {
    $req.GlobalSecondaryIndexes = $GlobalSecondaryIndexes
  }

  # Write compact JSON to a temp file to avoid quoting/escaping issues
  $json = $req | ConvertTo-Json -Depth 10 -Compress
  $tmp  = New-TemporaryFile
  [System.IO.File]::WriteAllText($tmp.FullName, $json, (New-Object System.Text.UTF8Encoding($false)))

  try {
    awsLocal dynamodb create-table --cli-input-json ("file://{0}" -f $tmp.FullName) 1>$null
    awsLocal dynamodb wait table-exists --table-name $TableName
    Write-Host "$TableName created and ACTIVE"
  } finally {
    Remove-Item $tmp.FullName -ErrorAction SilentlyContinue
  }
}

# 3) Create tables

# subjects: PK = subject_id (S)
# Attributes (at write-time): created_at (N), residency (S), erasure_in_progress (BOOL), erasure_requested_at (N), version (N)
Ensure-TableJson -TableName "subjects" `
  -AttributeDefinitions @(
    @{ AttributeName = "subject_id"; AttributeType = "S" }
  ) `
  -KeySchema @(
    @{ AttributeName = "subject_id"; KeyType = "HASH" }
  )

# policies: PK = purpose (S)
# Attributes: retention_days (N), description (S), last_updated_at (N)
Ensure-TableJson -TableName "policies" `
  -AttributeDefinitions @(
    @{ AttributeName = "purpose"; AttributeType = "S" }
  ) `
  -KeySchema @(
    @{ AttributeName = "purpose"; KeyType = "HASH" }
  )

# records: PK = subject_id (S), SK = record_key (S)
# Attributes (at write-time): purpose (S), value (S|M), version (N), created_at (N), updated_at (N),
#   tombstoned (BOOL), tombstoned_at (N), retention_days (N), purge_due_at (N), purge_bucket (S), request_id (S)
# GSI records_by_purge_due: PK = purge_bucket (S), SK = purge_due_at (N), Projection: KEYS_ONLY (or minimal)
$recordsAttributeDefs = @(
  @{ AttributeName = "subject_id";   AttributeType = "S" },
  @{ AttributeName = "record_key";   AttributeType = "S" },
  @{ AttributeName = "purge_bucket"; AttributeType = "S" }, # for GSI PK
  @{ AttributeName = "purge_due_at"; AttributeType = "N" }  # for GSI SK
)

$recordsKeySchema = @(
  @{ AttributeName = "subject_id"; KeyType = "HASH"  },
  @{ AttributeName = "record_key"; KeyType = "RANGE" }
)

$recordsGsi = @(
  @{
    IndexName = "records_by_purge_due"
    KeySchema = @(
      @{ AttributeName = "purge_bucket"; KeyType = "HASH"  },
      @{ AttributeName = "purge_due_at"; KeyType = "RANGE" }
    )
    Projection = @{
      ProjectionType = "KEYS_ONLY"
    }
  }
)

Ensure-TableJson -TableName "records" `
  -AttributeDefinitions $recordsAttributeDefs `
  -KeySchema $recordsKeySchema `
  -GlobalSecondaryIndexes $recordsGsi

# audit_events: PK = subject_id (S), SK = ts_ulid (S)
# Attributes: event_type (S), request_id (S), item_key (S, optional), purpose (S, optional),
#   timestamp (N), details (M), prev_hash (S), hash (S)
Ensure-TableJson -TableName "audit_events" `
  -AttributeDefinitions @(
    @{ AttributeName = "subject_id"; AttributeType = "S" },
    @{ AttributeName = "ts_ulid";    AttributeType = "S" }
  ) `
  -KeySchema @(
    @{ AttributeName = "subject_id"; KeyType = "HASH"  },
    @{ AttributeName = "ts_ulid";    KeyType = "RANGE" }
  )

Write-Host "All tables created / verified."

