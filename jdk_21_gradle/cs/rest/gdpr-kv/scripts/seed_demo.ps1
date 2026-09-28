#!/usr/bin/env pwsh
param(
    [string]$Region = "us-west-2",
    [string]$Endpoint = "http://localhost:4566"
)

$subjectId = "demo_subject_001"
$purpose = "DEMO_PURPOSE"
$retentionDays = 1

$now = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

Write-Host "Seeding demo data into $Endpoint (region $Region)"

aws --endpoint-url $Endpoint --region $Region dynamodb put-item --table-name subjects --item @""{
  "subject_id": {"S": "$subjectId"},
  "created_at": {"N": "$now"},
  "version": {"N": "1"},
  "residency": {"S": "US"}
}""@

aws --endpoint-url $Endpoint --region $Region dynamodb put-item --table-name policies --item @""{
  "purpose": {"S": "$purpose"},
  "retention_days": {"N": "$retentionDays"},
  "description": {"S": "Demo retention policy"},
  "last_updated_at": {"N": "$now"}
}""@

Write-Host "Seed complete. Policy purpose seeded: $purpose"
