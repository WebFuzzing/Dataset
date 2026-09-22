# GDPR Friendly Key Value Store

# Background

The General Data Protection Regulation (GDPR) is an EU law that sets rules for how personal data of EU residents is collected, stored, and used. It applies globally to any organization handling such data. GDPR emphasizes that data must be processed lawfully and transparently, kept accurate and minimal, stored only as long as necessary, secured against misuse, and managed in a way that organizations can demonstrate compliance.

Key requirements of GDPR:

* **Lawfulness, fairness, transparency:** users must know how data is used.
* **Purpose limitation:** only process data for explicit, legitimate purposes.
* **Data minimization:** store only what’s necessary.
* **Accuracy:** keep data correct and up to date.
* **Storage limitation:** delete when no longer needed.
* **Integrity and confidentiality:** secure the data.
* **Accountability:** you must be able to prove compliance.

# Problem Statement

DynamoDB is a fast, highly scalable key-value store, but it is not inherently GDPR compliant. GDPR requires operation to be data subject centric, while DynamoDB is inherently key-centric. This gap creates 4 distinct challenges:

1. **No subject view:** DynamoDB has no built-in way to list all records for a given subject, a subject to keys mapping must be maintained
2. **Erasure guarantees:** DeleteItem and TTLs mark data for removal, but erasure must be provably complete
3. **Retention policies**: DynamoDB TTL is best-effort and not precise enough for strict compliance
4. **Audit trial**: DynamoDB lacks immutable, tamper-evident logs out of the box

# In Scope Requirements

This project will build a GDPR-friendly key–value store layer on top of DynamoDB. The following requirements are in scope:

1. **Subject-Centric Mapping**
    * Maintain a subject-to-keys directory so all records associated with a given subject can be enumerated efficiently.
2. **Right to Erasure**  
   Support erasure requests that:
    * Immediately suppress reads (tombstoning).
    * Trigger background purge workflows to ensure physical removal.
    * Provide evidence of erasure completion.
3. **Retention Policies**
    * Implement reliable retention enforcement beyond DynamoDB’s best-effort TTL, ensuring records are deleted once no longer needed.
    * Generate audit events when retention deletes occur.
4. **Audit Trail**
    * Produce an immutable, tamper-evident log of all operations that touch personal data (create, read, update, delete, erasure).
    * Audit records must include actor, subject, operation type, timestamp, and request ID.

# Out of Scope

1. **Advanced Query Capabilities**
    * Full-text search, secondary filtering beyond key/subject lookup, or complex analytics on stored values.
2. **Data Anonymization / Pseudonymization**
    * Automatic anonymization or pseudonymization of personal data is not included.
3. **Differential Privacy & Advanced Privacy Techniques**
    * Techniques such as k-anonymity, l-diversity, or noise injection are not addressed.
4. **Dual-Control Approvals**
    * “Two-person rule” or multi-step approval workflows for export or erasure operations.
5. **Complex Residency Models**
    * No dynamic routing or multi-region replication policies are included.
6. **Legal/Organizational Processes**
    * The project does not cover consent collection, Data Protection Impact Assessments (DPIAs), or legal data processing agreements.
7. **Third-Party Integrations**
    * Direct integrations with external compliance monitoring, reporting, or case management tools.

# High Level Solution

This section defines the major operations supported by the KV store, the required validations, audit behavior, and background sweeper logic. All operations are **subject-centric** and backed by DynamoDB tables:

* `subjects` — per-subject metadata (existence, residency, flags).
* `policies` — purpose → retention mapping.
* `records` — all per-subject items (partitioned by subject).
* `audit_events` — append-only, tamper-evident audit trail.



##  **1\. CreateSubject**

**Flow:**

1. If the subject already exists, return an error (without leaking info).
2. Write an audit entry `CREATE_SUBJECT_REQUESTED`.
3. Insert a new row in `subjects`.
4. Write audit entry `CREATE_SUBJECT_COMPLETED`



## **2\. PutItem**

**Flow:**

1. Insert `PUT_REQUESTED` audit event.
2. Validate:
    * Subject exists (in `subjects`).
    * Purpose exists (in `policies`).
3. If validation fails:
    * Write `PUT_FAILED` audit event.
    * Return error.
4. If record doesn’t exist:
    * Insert new record.
    * Audit `PUT_NEW_ITEM_SUCCESS`.
5. If record exists:
    * Update record, increment version.
    * Audit `PUT_UPDATE_ITEM_SUCCESS`.


## **3\. GetItem**

**Flow:**

1. Audit `GET_REQUESTED`.
2. Validate: subject exists, item exists, item not tombstoned.
3. If validation fails:
    * Audit `GET_FAILURE`.
    * Return error.
4. Otherwise:
    * Return item.
    * Audit `GET_SUCCESS`.


## **4\. DeleteItem**

**Flow:**

1. Audit `DELETE_ITEM_REQUESTED`.
2. Validate subject exists and item exists.
    * If not: audit `DELETE_ITEM_FAILURE`.
3. If item already tombstoned:
    * Audit `DELETE_ITEM_ALREADY_TOMBSTONED`.
    * Return success (idempotent).
4. Otherwise:
    * Mark record tombstoned with timestamp.
    * Audit `DELETE_ITEM_SUCCESSFUL`.


## **5\. DeleteSubject**

**Flow:**

1. Audit `DELETE_SUBJECT_REQUESTED`.
2. Validate subject exists.
    * If not: audit `DELETE_SUBJECT_NO_SUBJECT`.  
      On other errors: audit `DELETE_SUBJECT_FAILURE`.
3. Query all records for subject.
4. Call `DeleteItem` on each.
5. Verify no non-tombstoned records remain.
6. Audit `DELETE_SUBJECT_SUCCESS`.



## **6\. Background PurgeSweeper**

**Purpose:** Permanently delete items after retention has expired.

**Flow:**

1. Runs every few minutes, scanning `records`.
2. If record is tombstoned and `tombstone_time + retention_days` has passed:
    * Audit `PURGE_CANDIDATE_IDENTIFIED`.
    * Delete item.
    * Audit `PURGE_CANDIDATE_SUCCESSFUL`.
3. If purge fails: audit `PURGE_CANDIDATE_FAILED`.


# Low Level Solution

## Data models

### **`subjects` (subject registry & flags)**

* **PK**: `subject_id` (S) — opaque, stable identifier for the data subject.

* **Attributes**
    * `created_at` (N, epoch millis)
    * `residency` (S, e.g., `"EU" | "US" | "UNKNOWN"`)
    * `erasure_in_progress` (BOOL)
    * `erasure_requested_at` (N, epoch millis) — when a subject‑wide erasure was requested
    * `version` (N) — optimistic concurrency if needed
* **Notes**
    * Item existence is the authoritative “subject exists” check.

### **`policies` (purpose → retention)**

* **PK**: `purpose` (S) — e.g., `"FULFILLMENT"`, `"MARKETING"`.
* **Attributes**
    * `retention_days` (N) — integer days
    * `description` (S)
    * `last_updated_at` (N)
* **Notes**
    * Admin‑only mutations

### **`records` (subject‑scoped items)**

* **PK**: `subject_id` (S)
* **SK**: `record_key` (S) — caller‑defined item key subject (“order\#123”, “pref:email”)

* **Attributes**
    * `purpose` (S) — must match a key in `policies`
    * `value` (M | S) — JSON object or string payload (encrypted at rest)
    * `version` (N) — increments on each update
    * `created_at` (N), `updated_at` (N)
    * `tombstoned` (BOOL, default false)
    * `tombstoned_at` (N, epoch millis) — when soft‑deleted
    * `retention_days` (N) — denormalized from `policies` at write time
    * `purge_due_at` (N) — `tombstoned_at + retention_days * 86400000`
    * `purge_bucket (N) - UTC hour bucket string derived from purge_due_at (e.g., h#20250827T21)`
    * `request_id` (S) — last write

**Indexes (new):**

* **GSI: `records_by_purge_due`**
    * PK: purge\_bucket (S)
    * SK: purge\_due\_at (N)
    * Projection: KEYS\_ONLY (or minimal fields)
    * Sparse: only populated for tombstoned records

**Notes**

* Subject→keys enumeration is a simple Query on `subject_id`.
* We deliberately avoid DynamoDB TTL for authoritative deletion.

### **`audit_events` (append‑only, tamper‑evident)**

* **PK**: `subject_id` (S) — allows per‑subject audit replay
* **SK**: `ts_ulid` (S) — `"{millis}_{ULID}"` for uniqueness & sort by time
* **Attributes**
    * `event_type` (S) — enum (see §3.6)
    * `request_id` (S)
    * `item_key` (S, optional) — when an item is involved
    * `purpose` (S, optional)
    * `timestamp` (N) — epoch millis
    * `details` (M) — minimal fields: status, error\_code, size, counts, etc.
    * `prev_hash` (S) — SHA‑256 of **previous** event’s `hash` (per subject)
    * `hash` (S) — SHA‑256 over canonical JSON of this event (incl. `prev_hash`)

**Tamper evidence**

* Maintain per‑subject hash chain via `prev_hash`.

## API Models

All requests/response bodies are JSON. Response `Content-Type: application/json`. Every request accepts/returns:

* **Headers in**:
    * `X-Request-Id` (optional; server generates if missing)
* **Headers out**:
    * `X-Request-Id` (echo)
    * `ETag` (when relevant; holds `version`)

### CreateSubject

**Command**:

| POST /subjects |
| :---- |

#### Happy Path

Input

| {  "subject\_id": "sub\_123", "residency": "EU", "flags": { "blocked": false }} |
| :---- |

Success (201 Created on first create; 200 OK if idempotent re-create)

| {  "subject\_id": "sub\_123",  "created\_at": 1724512300000,  "residency": "EU"} |
| :---- |

#### Unhappy Path

400 VALIDATION\_FAILED

| { "error": "VALIDATION\_FAILED", "message": "subject\_id must be non-empty" } |
| :---- |

500 INTERNAL\_ERROR

| { "error": "INTERNAL\_ERROR", "message": "Unexpected server error. Try again later." } |
| :---- |

### PutItem

**Command**:

| PUT /subjects/sub\_123/records/pref:email |
| :---- |

#### Happy Path

Input

| {  "purpose": "FULFILLMENT",    "subject\_id": “sub\_123”,   "record\_key": “perf:email”,   "value": { "email": "jess@example.com" } }} |
| :---- |

Success (200 OK)

| {  "subject\_id": "sub\_123",  "record\_key": "pref:email",  "version": 1,  "updated\_at": 1724512399000} |
| :---- |

#### Unhappy Path

**404 SUBJECT\_NOT\_FOUND**

| { "error": "SUBJECT\_NOT\_FOUND", "message": "Subject sub\_123 not found" } |
| :---- |

**400 INVALID\_PURPOSE**

| { "error": "INVALID\_PURPOSE", "message": "Purpose UNKNOWN\_PURPOSE is not configured" } |
| :---- |

**500 INTERNAL\_ERROR**

| { "error": "INTERNAL\_ERROR", "message": "Unexpected server error. Try again later." } |
| :---- |

Table updates (errors)

### GetItem

#### Happy Path

Input

| {  "subject\_id": "sub\_123",  "record\_key": "pref:email",} |
| :---- |

Success (200 OK)

| {  "subject\_id": "sub\_123",  "record\_key": "pref:email",  "version": 1,  "purpose": "FULFILLMENT",  "value": { "email": "jess@example.com" },  "updated\_at": 1724512399000} |
| :---- |

#### Unhappy Path

**404 SUBJECT\_NOT\_FOUND**

| { "error": "SUBJECT\_NOT\_FOUND", "message": "Subject sub\_123 not found" } |
| :---- |

**404 RECORD\_NOT\_FOUND**

| { "error": "RECORD\_NOT\_FOUND", "message": "Record pref:email not found for subject sub\_123" } |
| :---- |

**410 READ\_SUPPRESSED\_TOMBSTONE**

| { "error": "READ\_SUPPRESSED\_TOMBSTONE", "message": "Item pref:email is tombstoned" } |
| :---- |

**500 INTERNAL\_ERROR**

| { "error": "INTERNAL\_ERROR", "message": "Unexpected server error. Try again later." } |
| :---- |

### DeleteItem

**Command**: `DELETE /subjects/sub_123/records/pref:email`

#### Happy Paths

Input

| {  "subject\_id": "sub\_123",  "record\_key": "pref:email",} |
| :---- |

##### Success (200 OK) — first tombstone

| {  "subject\_id": "sub\_123",  "record\_key": "pref:email",  "tombstoned": true,  "tombstoned\_at": 1724512405000,  "purge\_due\_at": 1727114405000} |
| :---- |

##### Success (200 OK) — already tombstoned (idempotent)

| {  "subject\_id": "sub\_123",  "record\_key": "pref:email",  "tombstoned": true,   "tombstoned\_at": 1724512405000,  "purge\_due\_at": 1727114405000} |
| :---- |

#### Unhappy Path

**404 SUBJECT\_NOT\_FOUND**

| { "error": "SUBJECT\_NOT\_FOUND", "message": "Subject sub\_123 not found" } |
| :---- |

**404 RECORD\_NOT\_FOUND**

| { "error": "RECORD\_NOT\_FOUND", "message": "Record perf:email for subject does not exist." } |
| :---- |

**500 INTERNAL\_ERROR**

| { "error": "INTERNAL\_ERROR", "message": "Unexpected server error. Try again later." } |
| :---- |

### DeleteSubject

**Command**: `DELETE /subjects/sub_123`

Input

| { "subject\_id": "sub\_123" } |
| :---- |

#### Happy Path

Success (200 OK)

| {  "subject\_id": "sub\_123",  "erasure\_in\_progress": true} |
| :---- |

#### Unhappy Path

**404 SUBJECT\_NOT\_FOUND**

| { "error": "SUBJECT\_NOT\_FOUND", "message": "Subject sub\_123 not found" } |
| :---- |

**500 INTERNAL\_ERROR**

| { "error": "INTERNAL\_ERROR", "message": "Unexpected server error. Try again later." } |
| :---- |

## 

### **Background PurgeSweeper — Low Level Design (new section)**

**Purpose:** Permanently delete items after retention has expired, using the purge\_bucket \+ purge\_due\_at GSI to efficiently find due candidates.

**Flow:**

* Runs every few minutes (EventBridge or cron).

* Builds candidate list of purge\_buckets covering `(now – lookback_window, now + 1m)`.

* For each bucket:
    * Query `records_by_purge_due` with condition `purge_due_at <= now`.
    * For each candidate:
        * Emit audit event `PURGE_CANDIDATE_IDENTIFIED`.
        * Execute a `TransactWrite` with:
            1. ConditionCheck: `tombstoned = true AND purge_due_at <= now`  
               Delete the record  
               Put audit event `PURGE_CANDIDATE_SUCCESSFUL`
        * On conditional failure: emit `PURGE_CANDIDATE_FAILED`.

**Idempotency & concurrency:**

* Safe to run in parallel; conditional checks prevent double deletion.
* Each audit event keyed with unique `ts_ulid`.

**Table updates per candidate:**

* `records`: DeleteItem (permanent removal)
* `audit_events`: PutItem (`PURGE_CANDIDATE_IDENTIFIED`, then `PURGE_CANDIDATE_SUCCESSFUL` or `PURGE_CANDIDATE_FAILED`)

**Metrics:**

* candidates\_scanned
* purged\_ok
* purge\_failed
* purge\_latency\_ms

**Error handling:**

* Throttling → exponential backoff with jitter
* ConditionalCheckFailed → item already gone or revived, log `FAILED`
* Non-retryable → `PURGE_CANDIDATE_FAILED`



