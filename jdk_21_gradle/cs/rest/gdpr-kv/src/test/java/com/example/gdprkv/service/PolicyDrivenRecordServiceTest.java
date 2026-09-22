package com.example.gdprkv.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.gdprkv.access.PolicyAccess;
import com.example.gdprkv.access.RecordAccess;
import com.example.gdprkv.access.SubjectAccess;
import com.example.gdprkv.models.Policy;
import com.example.gdprkv.models.Record;
import com.example.gdprkv.models.Subject;
import com.example.gdprkv.requests.PutRecordServiceRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PolicyDrivenRecordServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private InMemoryPolicyAccess policyAccess;
    private InMemoryRecordAccess recordAccess;
    private InMemorySubjectAccess subjectAccess;
    private Clock clock;
    private PolicyDrivenRecordService service;

    @BeforeEach
    void setUp() {
        policyAccess = new InMemoryPolicyAccess();
        recordAccess = new InMemoryRecordAccess();
        subjectAccess = new InMemorySubjectAccess();
        clock = Clock.fixed(Instant.parse("2024-09-01T12:00:00Z"), ZoneOffset.UTC);
        service = new PolicyDrivenRecordService(policyAccess, recordAccess, subjectAccess, clock);
    }

    @Test
    @DisplayName("Creates new record using policy retention and timestamps")
    void putRecordNewItem() throws Exception {
        policyAccess.save(policy("FULFILLMENT", 30));
        subjectAccess.save(subject("sub_1"));

        PutRecordServiceRequest request = new PutRecordServiceRequest(
                "sub_1",
                "pref:email",
                "FULFILLMENT",
                MAPPER.readTree("{\"email\":\"demo@example.com\"}")
        );

        Record saved = service.putRecord(request);

        assertEquals("sub_1", saved.getSubjectId());
        assertEquals("pref:email", saved.getRecordKey());
        assertEquals("FULFILLMENT", saved.getPurpose());
        assertEquals(30, saved.getRetentionDays());
        assertEquals(clock.millis(), saved.getCreatedAt());
        assertEquals(clock.millis(), saved.getUpdatedAt());
        assertEquals(1L, saved.getVersion());
        assertFalse(saved.getTombstoned());
        assertFalse(saved.getRequestId() == null || saved.getRequestId().isBlank());
        assertNotNull(recordAccess.findBySubjectIdAndRecordKey("sub_1", "pref:email"));
    }

    @Test
    @DisplayName("Updates existing record and computes tombstone metadata")
    void putRecordExistingTombstone() throws Exception {
        policyAccess.save(policy("DELETION", 10));
        subjectAccess.save(subject("sub_2"));

        Record existing = Record.builder()
                .subjectId("sub_2")
                .recordKey("pref:sms")
                .purpose("DELETION")
                .value(MAPPER.readTree("{\"enabled\":true}"))
                .createdAt(Instant.parse("2024-08-01T00:00:00Z").toEpochMilli())
                .updatedAt(Instant.parse("2024-08-02T00:00:00Z").toEpochMilli())
                .version(3L)
                .requestId("req-old")
                .retentionDays(10)
                .tombstoned(false)
                .build();
        recordAccess.save(existing);

        PutRecordServiceRequest request = new PutRecordServiceRequest(
                "sub_2",
                "pref:sms",
                "DELETION",
                MAPPER.readTree("{\"enabled\":false}"),
                true,
                null,
                null
        );

        Record saved = service.putRecord(request);

        assertEquals(Instant.parse("2024-08-01T00:00:00Z").toEpochMilli(), saved.getCreatedAt());
        assertEquals(clock.millis(), saved.getUpdatedAt());
        assertEquals(4L, saved.getVersion());
        assertTrue(saved.getTombstoned());
        assertNotNull(saved.getTombstonedAt());
        assertEquals(Record.calculatePurgeDueAt(saved.getTombstonedAt(), 10), saved.getPurgeDueAt());
        assertEquals(Record.formatPurgeBucket(saved.getPurgeDueAt()), saved.getPurgeBucket());
    }

    @Test
    @DisplayName("Throws when policy missing")
    void putRecordMissingPolicy() throws Exception {
        subjectAccess.save(subject("sub_3"));
        PutRecordServiceRequest request = new PutRecordServiceRequest(
                "sub_3",
                "pref:email",
                "UNKNOWN",
                MAPPER.readTree("{}")
        );

        GdprKvException ex = assertThrows(GdprKvException.class, () -> service.putRecord(request));
        assertEquals(GdprKvException.Code.INVALID_PURPOSE, ex.getCode());
    }

    @Test
    @DisplayName("Throws when subject missing")
    void putRecordMissingSubject() throws Exception {
        policyAccess.save(policy("FULFILLMENT", 30));

        PutRecordServiceRequest request = new PutRecordServiceRequest(
                "absent",
                "pref:email",
                "FULFILLMENT",
                MAPPER.readTree("{}")
        );

        GdprKvException ex = assertThrows(GdprKvException.class, () -> service.putRecord(request));
        assertEquals(GdprKvException.Code.SUBJECT_NOT_FOUND, ex.getCode());
    }

    private Policy policy(String purpose, int retentionDays) {
        return Policy.builder()
                .purpose(purpose)
                .retentionDays(retentionDays)
                .description("demo")
                .lastUpdatedAt(clock.millis())
                .build();
    }

    private Subject subject(String subjectId) {
        return Subject.builder()
                .subjectId(subjectId)
                .createdAt(clock.millis())
                .requestId("test-req")
                .build();
    }

    private static class InMemoryPolicyAccess implements PolicyAccess {
        private final Map<String, Policy> store = new HashMap<>();

        void save(Policy policy) {
            store.put(policy.getPurpose(), policy);
        }

        @Override
        public Optional<Policy> findByPurpose(String purpose) {
            return Optional.ofNullable(store.get(purpose));
        }
    }

    private static class InMemoryRecordAccess implements RecordAccess {
        private final Map<String, Record> store = new HashMap<>();

        @Override
        public Optional<Record> findBySubjectIdAndRecordKey(String subjectId, String recordKey) {
            return Optional.ofNullable(store.get(key(subjectId, recordKey)));
        }

        @Override
        public List<Record> findAllBySubjectId(String subjectId) {
            return store.values().stream()
                    .filter(r -> r.getSubjectId().equals(subjectId))
                    .sorted(java.util.Comparator.comparing(Record::getRecordKey))
                    .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public List<Record> findRecordsDueForPurge(String purgeBucket, long cutoffTimestamp) {
            return store.values().stream()
                    .filter(r -> r.getPurgeBucket() != null && r.getPurgeBucket().equals(purgeBucket))
                    .filter(r -> r.getPurgeDueAt() != null && r.getPurgeDueAt() <= cutoffTimestamp)
                    .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public Record save(Record record) {
            store.put(key(record.getSubjectId(), record.getRecordKey()), record);
            return record;
        }

        @Override
        public void delete(Record record) {
            store.remove(key(record.getSubjectId(), record.getRecordKey()));
        }

        private String key(String subjectId, String recordKey) {
            return subjectId + "#" + recordKey;
        }
    }

    private static class InMemorySubjectAccess implements SubjectAccess {
        private final Map<String, Subject> store = new HashMap<>();

        @Override
        public Optional<Subject> findBySubjectId(String subjectId) {
            return Optional.ofNullable(store.get(subjectId));
        }

        @Override
        public Subject save(Subject subject) {
            store.put(subject.getSubjectId(), subject);
            return subject;
        }

        @Override
        public Subject update(Subject subject) {
            store.put(subject.getSubjectId(), subject);
            return subject;
        }
    }
}
