#!/usr/bin/env bash
set -euo pipefail

REGION="us-west-2"
ENDPOINT="http://localhost:4566"
SUBJECT_ID="demo_subject_001"
RECORD_KEY="pref:email"
PURPOSE="DEMO_PURPOSE"
REQUEST_ID="seed-demo"
RETENTION_DAYS="${RETENTION_DAYS:-1}"

: "${AWS_ACCESS_KEY_ID:=test}"
: "${AWS_SECRET_ACCESS_KEY:=test}"
export AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY

usage() {
  cat <<'USAGE'
Usage: seed_demo.sh [--region REGION] [--endpoint URL]

Seeds LocalStack with a demo subject and policy for local testing.
  --region, -r    AWS region (default: us-west-2)
  --endpoint, -e  DynamoDB endpoint URL (default: http://localhost:4566)
  --help,   -h    Show this help message
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --region|-r)
      REGION="$2"; shift 2 ;;
    --endpoint|--endpoint-url|-e)
      ENDPOINT="$2"; shift 2 ;;
    --help|-h)
      usage; exit 0 ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1 ;;
  esac
done

aws_local() {
  aws --endpoint-url "$ENDPOINT" --region "$REGION" "$@"
}

created_at=$(python3 - <<'PY'
import time
print(int(time.time() * 1000))
PY
)
updated_at="$created_at"

printf 'Seeding demo data into %s (region %s)\n' "$ENDPOINT" "$REGION"

subject_item=$(cat <<JSON
{
  "subject_id": {"S": "$SUBJECT_ID"},
  "created_at": {"N": "$created_at"},
  "version": {"N": "1"},
  "residency": {"S": "US"}
}
JSON
)

policy_item=$(cat <<JSON
{
  "purpose": {"S": "$PURPOSE"},
  "retention_days": {"N": "$RETENTION_DAYS"},
  "description": {"S": "Demo retention policy"},
  "last_updated_at": {"N": "$created_at"}
}
JSON
)

aws_local dynamodb put-item --table-name subjects --item "$subject_item"
aws_local dynamodb put-item --table-name policies --item "$policy_item"

printf 'Seed complete. Policy purpose seeded: %s\n' "$PURPOSE"
