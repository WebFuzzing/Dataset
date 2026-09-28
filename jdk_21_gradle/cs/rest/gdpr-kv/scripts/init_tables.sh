#!/usr/bin/env bash
set -euo pipefail

REGION="us-west-2"
ENDPOINT="http://localhost:4566"

usage() {
  cat <<'USAGE'
Usage: init_tables.sh [--region REGION] [--endpoint URL]
  --region, -r    AWS region to target (default: us-west-2)
  --endpoint, -e  AWS endpoint URL (default: http://localhost:4566)
  --help,   -h    Show this help message
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --region|-r)
      [[ $# -ge 2 ]] || { echo "Missing value for $1" >&2; usage; exit 1; }
      REGION="$2"
      shift 2
      ;;
    --endpoint|--endpoint-url|-e)
      [[ $# -ge 2 ]] || { echo "Missing value for $1" >&2; usage; exit 1; }
      ENDPOINT="$2"
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

aws_local() {
  aws --endpoint-url "$ENDPOINT" --region "$REGION" "$@"
}

printf 'Using AWS endpoint %s (region %s)\n' "$ENDPOINT" "$REGION"
printf 'Waiting for DynamoDB to be ready...\n'

max_attempts=40
for ((i=0; i<max_attempts; i++)); do
  if aws_local dynamodb list-tables >/dev/null 2>&1; then
    break
  fi
  if (( i == max_attempts - 1 )); then
    printf 'DynamoDB not responding on %s\n' "$ENDPOINT" >&2
    exit 1
  fi
  sleep 1
done

ensure_table() {
  local table_name=$1
  local json_definition=$2

  if aws_local dynamodb describe-table --table-name "$table_name" >/dev/null 2>&1; then
    printf '%s already exists\n' "$table_name"
    return
  fi

  printf 'Creating %s...\n' "$table_name"
  local tmp
  tmp=$(mktemp)
  printf '%s\n' "$json_definition" >"$tmp"

  aws_local dynamodb create-table --cli-input-json "file://$tmp" >/dev/null
  aws_local dynamodb wait table-exists --table-name "$table_name"
  printf '%s created and ACTIVE\n' "$table_name"

  rm -f "$tmp"
}

ensure_table "subjects" "$(cat <<'JSON'
{
  "TableName": "subjects",
  "BillingMode": "PAY_PER_REQUEST",
  "AttributeDefinitions": [
    { "AttributeName": "subject_id", "AttributeType": "S" }
  ],
  "KeySchema": [
    { "AttributeName": "subject_id", "KeyType": "HASH" }
  ]
}
JSON
)"

ensure_table "policies" "$(cat <<'JSON'
{
  "TableName": "policies",
  "BillingMode": "PAY_PER_REQUEST",
  "AttributeDefinitions": [
    { "AttributeName": "purpose", "AttributeType": "S" }
  ],
  "KeySchema": [
    { "AttributeName": "purpose", "KeyType": "HASH" }
  ]
}
JSON
)"

ensure_table "records" "$(cat <<'JSON'
{
  "TableName": "records",
  "BillingMode": "PAY_PER_REQUEST",
  "AttributeDefinitions": [
    { "AttributeName": "subject_id", "AttributeType": "S" },
    { "AttributeName": "record_key", "AttributeType": "S" },
    { "AttributeName": "purge_bucket", "AttributeType": "S" },
    { "AttributeName": "purge_due_at", "AttributeType": "N" }
  ],
  "KeySchema": [
    { "AttributeName": "subject_id", "KeyType": "HASH" },
    { "AttributeName": "record_key", "KeyType": "RANGE" }
  ],
  "GlobalSecondaryIndexes": [
    {
      "IndexName": "records_by_purge_due",
      "KeySchema": [
        { "AttributeName": "purge_bucket", "KeyType": "HASH" },
        { "AttributeName": "purge_due_at", "KeyType": "RANGE" }
      ],
      "Projection": {
        "ProjectionType": "KEYS_ONLY"
      }
    }
  ]
}
JSON
)"

ensure_table "audit_events" "$(cat <<'JSON'
{
  "TableName": "audit_events",
  "BillingMode": "PAY_PER_REQUEST",
  "AttributeDefinitions": [
    { "AttributeName": "subject_id", "AttributeType": "S" },
    { "AttributeName": "ts_ulid", "AttributeType": "S" }
  ],
  "KeySchema": [
    { "AttributeName": "subject_id", "KeyType": "HASH" },
    { "AttributeName": "ts_ulid", "KeyType": "RANGE" }
  ]
}
JSON
)"

printf 'All tables created / verified.\n'
