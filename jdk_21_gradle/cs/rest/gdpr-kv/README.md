# GDPR-Friendly Key Value Store

[![CI](https://github.com/jessicant/GDPR-KV/actions/workflows/ci.yml/badge.svg)](https://github.com/jessicant/GDPR-KV/actions/workflows/ci.yml)

A subject-centric layer on DynamoDB that adds GDPR-oriented behaviors: right to erasure, strict retention, and a tamper-evident audit trail.

## Features

- **Subject-Centric Mapping** – enumerate all records for a data subject via `findAllBySubjectId()`.
- **Right to Erasure** – immediate read suppression (tombstone) plus background purge sweeper for physical deletion.
- **Automated Purge Sweeper** – scheduled job permanently deletes tombstoned records after retention expires using efficient GSI queries.
- **Retention Enforcement** – records deleted when retention expires; scheduled job deletes audit events older than configured retention period (default: 2 years).
- **Audit Trail** – append-only, hash-chained events for create, read, update, delete, and purge; retrieve complete audit history via `findAllBySubjectId()`.
- **Efficient Purging** – GSI `records_by_purge_due` uses `purge_bucket` (UTC hour shard) and `purge_due_at` to avoid scans or hot partitions.
- **Request Tracking** – all operations tagged with `X-Request-Id` header for end-to-end traceability.
- **GDPR Compliance** – minimal personal data in audit logs (no residency), configurable retention policies.

## Architecture

Tables:
- `subjects` – subject metadata (existence, residency, erasure flags).
- `policies` – purpose → retention mapping.
- `records` – per-subject items; includes `tombstoned`, `purge_due_at`, `purge_bucket`.
- `audit_events` – append-only, tamper-evident audit log.

Index:
- `records_by_purge_due` (GSI):
    - Partition key = `purge_bucket`
    - Sort key = `purge_due_at`
    - Sparse (only set for tombstoned records)

---

## Local Development

### Prerequisites
- Docker Desktop
- AWS CLI (and optionally [`awslocal`](https://github.com/localstack/awscli-local))

### Start LocalStack
```bash
docker compose up -d
```

### Set AWS Credentials
Export LocalStack credentials (they can be anything, `test` is conventional):
```bash
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
```
Windows PowerShell:
```powershell
$env:AWS_ACCESS_KEY_ID = "test"
$env:AWS_SECRET_ACCESS_KEY = "test"
```

> **Note:** These credentials are required for the AWS CLI to work, even though LocalStack doesn't validate them. Set them once per shell session.

### Initialize Tables
Run the provided script:
```bash
./scripts/init_tables.sh
```
Windows PowerShell:
```
./scripts/init_tables.ps1
```

This creates the following DynamoDB tables in LocalStack:
- `subjects`
- `policies`
- `records` (with GSI `records_by_purge_due`)
- `audit_events`

### Seed Demo Data
Run the seed script (creates the demo subject and policy `DEMO_PURPOSE`):
```bash
./scripts/seed_demo.sh
```
Windows PowerShell:
```powershell
./scripts/seed_demo.ps1
```

### Start the Application
Start the Spring Boot API locally (default port 8080):
```bash
./gradlew bootRun
```

Leave this running in a terminal. Once you see `Started GdprKvApplication`, the API is ready to accept requests.

### Create a Subject (via API)
Subjects must exist before records can be written for them. The demo seed already creates
`demo_subject_001`, but you can create your own subjects through the API:

```bash
curl -X PUT \
  -H "Content-Type: application/json" \
  http://localhost:8080/subjects/customer_123 \
  -d '{
    "residency": "US"
  }'
```

The request body is optional—omitting it creates a subject with default metadata. Reissuing the PUT
for an existing subject returns a `409 SUBJECT_ALREADY_EXISTS` error; subjects are immutable once
created.

### Put a Record (via API)
Once the subject exists (either via the seed script or the endpoint above) you can write a record
through the API. The service will return `404 SUBJECT_NOT_FOUND` if you attempt to write to a
non-existent subject.

With LocalStack and the app running, issue the request:

```bash
curl -X PUT \
  -H "Content-Type: application/json" \
  http://localhost:8080/subjects/demo_subject_001/records/pref:email \
  -d '{
    "purpose": "DEMO_PURPOSE",
    "value": { "email": "demo@example.com" }
  }'
```

Sample response:
```json
{
  "subject_id": "demo_subject_001",
  "record_key": "pref:email",
  "purpose": "DEMO_PURPOSE",
  "value": { "email": "demo@example.com" },
  "version": 2,
  "created_at": 1761258574353,
  "updated_at": 1761431775441,
  "retention_days": 1
}
```

The server generates an `X-Request-Id` (for audit and traceability) and returns it in the response headers; the JSON payload contains only the record fields.

### Retrieve All Data for a Subject
The API provides endpoints for retrieving all records and audit events for a subject to support GDPR subject access requests (Article 15).

#### Get All Records
Retrieve all records for a subject:

```bash
curl http://localhost:8080/subjects/demo_subject_001/records
```

Sample response:
```json
[
  {
    "subject_id": "demo_subject_001",
    "record_key": "pref:email",
    "purpose": "DEMO_PURPOSE",
    "value": { "email": "demo@example.com" },
    "version": 1,
    "created_at": 1761258574353,
    "updated_at": 1761258574353,
    "retention_days": 1
  },
  {
    "subject_id": "demo_subject_001",
    "record_key": "pref:theme",
    "purpose": "DEMO_PURPOSE",
    "value": { "theme": "dark" },
    "version": 1,
    "created_at": 1761258584422,
    "updated_at": 1761258584422,
    "retention_days": 1
  }
]
```

Records are returned in alphabetical order by `record_key`.

#### Get Audit Trail
Retrieve complete audit history for a subject:

```bash
curl http://localhost:8080/subjects/demo_subject_001/audit-events
```

Sample response:
```json
[
  {
    "subject_id": "demo_subject_001",
    "ts_ulid": "1727856000000_01J9K7G8H9M2N3P4Q5R6S7T8V9",
    "event_type": "PUT_REQUESTED",
    "request_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "timestamp": 1727856000000,
    "prev_hash": "0000000000000000000000000000000000000000000000000000000000000000",
    "hash": "abc123...",
    "item_key": "pref:email",
    "purpose": "DEMO_PURPOSE"
  },
  {
    "subject_id": "demo_subject_001",
    "ts_ulid": "1727856000000_01J9K7G8H9M2N3P4Q5R6S7T8V9",
    "event_type": "PUT_NEW_ITEM_SUCCESS",
    "request_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "timestamp": 1727856000000,
    "prev_hash": "abc123...",
    "hash": "def456...",
    "item_key": "pref:email",
    "purpose": "DEMO_PURPOSE",
    "details": { "version": 1 }
  }
]
```

Events are returned in chronological order (oldest first). Each event includes `hash` and `prev_hash` fields that form a tamper-evident chain.

### Delete a Record
Delete (tombstone) a record to implement the right to erasure. The record is marked for deletion and scheduled for purging based on its retention policy:

```bash
curl -X DELETE http://localhost:8080/subjects/demo_subject_001/records/pref:email
```

Sample response:
```json
{
  "subject_id": "demo_subject_001",
  "record_key": "pref:email",
  "purpose": "DEMO_PURPOSE",
  "value": { "email": "demo@example.com" },
  "version": 2,
  "created_at": 1761258574353,
  "updated_at": 1761743879873,
  "retention_days": 1
}
```

What happens when you delete a record:
- The record is **tombstoned** (marked as deleted but not physically removed)
- A `purge_due_at` timestamp is calculated based on the retention policy
- The record is indexed in the `records_by_purge_due` GSI for efficient sweeper processing
- Audit events are created: `DELETE_ITEM_REQUESTED` and `DELETE_ITEM_SUCCESSFUL`
- If you delete an already-tombstoned record, it returns successfully without changes and creates a `DELETE_ITEM_ALREADY_TOMBSTONED` audit event

The response includes all record fields. The actual tombstone metadata (`tombstoned`, `tombstoned_at`, `purge_due_at`, `purge_bucket`) is stored in DynamoDB but not included in the API response.

### Delete a Subject (Right to Erasure)
Delete a subject and all associated records to implement GDPR Article 17 (Right to Erasure). This operation marks the subject for erasure and tombstones all records:

```bash
curl -X DELETE http://localhost:8080/subjects/demo_subject_001
```

Sample response:
```json
{
  "subject": {
    "subject_id": "demo_subject_001",
    "created_at": 1727856000000,
    "residency": "US",
    "erasure_in_progress": true,
    "erasure_requested_at": 1727866000000
  },
  "records_deleted": 2,
  "total_records": 2
}
```

What happens when you delete a subject:
- The subject is **marked for erasure** with `erasure_in_progress=true` and `erasure_requested_at` timestamp
- **All associated records are tombstoned** immediately
- Each record is marked with `tombstoned=true`, `tombstoned_at` timestamp, and scheduled for purging
- Audit events are created: `SUBJECT_ERASURE_REQUESTED`, `SUBJECT_ERASURE_STARTED`, and `SUBJECT_ERASURE_COMPLETED`
- The response includes the count of records deleted and total records found
- If the subject doesn't exist, returns `404 SUBJECT_NOT_FOUND`

This ensures immediate read suppression of all data while maintaining audit trail compliance.

### Configure Purge Sweeper (Background Deletion)
The purge sweeper permanently deletes tombstoned records after their retention period expires. This is **required for GDPR compliance** to ensure physical deletion of personal data.

1. Edit `src/main/resources/application.yml`:
   ```yaml
   purge:
     sweeper:
       enabled: true  # Enable automatic purging
       schedule: "0 */15 * * * *"  # Every 15 minutes (cron format)
       lookback-hours: 24  # Number of hours of purge buckets to check
   ```

2. The sweeper will run on the configured schedule and:
   - Query the `records_by_purge_due` GSI for expired records
   - Permanently delete records past their `purge_due_at` timestamp
   - Create audit events: `PURGE_CANDIDATE_IDENTIFIED`, `PURGE_CANDIDATE_SUCCESSFUL`, or `PURGE_CANDIDATE_FAILED`
   - Process records in hourly buckets to avoid hot partitions

The sweeper uses safety checks to ensure only tombstoned records past their retention are deleted.

### Configure Audit Log Retention
Audit logs are retained for 2 years by default. To enable automatic deletion:

1. Edit `src/main/resources/application.yml`:
   ```yaml
   audit:
     retention:
       enabled: true  # Enable scheduled deletion job
       schedule: "0 0 2 * * *"  # Daily at 2am (cron format)
       retention-days: 730  # 2 years
   ```

2. The scheduled job will run automatically and delete audit events older than the retention period.

### Verify Results
Check that the record is gone:
```bash
awslocal dynamodb get-item \
  --table-name records \
  --key '{"subject_id":{"S":"demo_subject_001"},"record_key":{"S":"pref:email"}}'
```

Query the audit log to confirm purge events:
```bash
awslocal dynamodb query \
  --table-name audit_events \
  --key-condition-expression "subject_id=:s" \
  --expression-attribute-values '{":s":{"S":"demo_subject_001"}}' \
  --scan-index-forward false
```

## Documentation
The full design (detailed flows, API shapes, and low-level sweeper design lives here): [Design Document](./doc/design.md)
