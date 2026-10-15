#!/bin/bash

# The SUT does not create its DynamoDB tables, and it has no endpoint to create a policy:
# without a seeded purpose no record can ever be written. LocalStack runs this once it is ready.

set -e

awslocal dynamodb create-table --table-name subjects \
  --billing-mode PAY_PER_REQUEST \
  --attribute-definitions AttributeName=subject_id,AttributeType=S \
  --key-schema AttributeName=subject_id,KeyType=HASH

awslocal dynamodb create-table --table-name policies \
  --billing-mode PAY_PER_REQUEST \
  --attribute-definitions AttributeName=purpose,AttributeType=S \
  --key-schema AttributeName=purpose,KeyType=HASH

awslocal dynamodb create-table --table-name records \
  --billing-mode PAY_PER_REQUEST \
  --attribute-definitions AttributeName=subject_id,AttributeType=S \
                          AttributeName=record_key,AttributeType=S \
                          AttributeName=purge_bucket,AttributeType=S \
                          AttributeName=purge_due_at,AttributeType=N \
  --key-schema AttributeName=subject_id,KeyType=HASH \
               AttributeName=record_key,KeyType=RANGE \
  --global-secondary-indexes '[{
        "IndexName": "records_by_purge_due",
        "KeySchema": [{"AttributeName": "purge_bucket", "KeyType": "HASH"},
                      {"AttributeName": "purge_due_at", "KeyType": "RANGE"}],
        "Projection": {"ProjectionType": "KEYS_ONLY"}
      }]'

awslocal dynamodb create-table --table-name audit_events \
  --billing-mode PAY_PER_REQUEST \
  --attribute-definitions AttributeName=subject_id,AttributeType=S \
                          AttributeName=ts_ulid,AttributeType=S \
  --key-schema AttributeName=subject_id,KeyType=HASH \
               AttributeName=ts_ulid,KeyType=RANGE

awslocal dynamodb put-item --table-name policies --item '{
  "purpose": {"S": "DEMO_PURPOSE"},
  "retention_days": {"N": "1"},
  "description": {"S": "Demo retention policy"},
  "last_updated_at": {"N": "1700000000000"}
}'

awslocal dynamodb put-item --table-name subjects --item '{
  "subject_id": {"S": "demo_subject_001"},
  "created_at": {"N": "1700000000000"},
  "residency": {"S": "US"},
  "request_id": {"S": "seed"}
}'

awslocal dynamodb put-item --table-name records --item '{
  "subject_id": {"S": "demo_subject_001"},
  "record_key": {"S": "pref:email"},
  "purpose": {"S": "DEMO_PURPOSE"},
  "value": {"S": "{\"email\":\"demo@example.invalid\"}"},
  "created_at": {"N": "1700000000000"},
  "updated_at": {"N": "1700000000000"},
  "version": {"N": "1"},
  "retention_days": {"N": "1"},
  "tombstoned": {"BOOL": false},
  "request_id": {"S": "seed"}
}'

echo "WFD: DynamoDB tables created and demo data seeded"
