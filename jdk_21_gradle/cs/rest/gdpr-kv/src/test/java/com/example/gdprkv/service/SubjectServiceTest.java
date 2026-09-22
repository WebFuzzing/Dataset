package com.example.gdprkv.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.gdprkv.access.SubjectAccess;
import com.example.gdprkv.models.Subject;
import com.example.gdprkv.requests.PutSubjectServiceRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

class SubjectServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2024-10-01T00:00:00Z"), ZoneOffset.UTC);

    private InMemorySubjectAccess subjectAccess;
    private SubjectService subjectService;

    @BeforeEach
    void setUp() {
        subjectAccess = new InMemorySubjectAccess();
        subjectService = new SubjectService(subjectAccess, CLOCK);
    }

    @Test
    @DisplayName("creates a new subject with defaults")
    void putSubjectCreatesNew() {
        Subject subject = subjectService.putSubject(new PutSubjectServiceRequest("demo", "US", "req-123"));

        assertEquals("demo", subject.getSubjectId());
        assertEquals(CLOCK.millis(), subject.getCreatedAt());
        assertEquals("US", subject.getResidency());
        assertEquals("req-123", subject.getRequestId());
        assertTrue(subjectService.exists("demo"));
    }

    @Test
    @DisplayName("throws when subject already exists")
    void putSubjectRejectsExisting() {
        Subject existing = Subject.builder()
                .subjectId("demo")
                .createdAt(123L)
                .residency("GB")
                .requestId("existing-req")
                .build();
        subjectAccess.put(existing);

        GdprKvException ex = assertThrows(GdprKvException.class,
                () -> subjectService.putSubject(new PutSubjectServiceRequest("demo", "US", "req-456")));
        assertEquals(GdprKvException.Code.SUBJECT_ALREADY_EXISTS, ex.getCode());
    }

    private static class InMemorySubjectAccess implements SubjectAccess {
        private final Map<String, Subject> store = new HashMap<>();

        @Override
        public Optional<Subject> findBySubjectId(String subjectId) {
            return Optional.ofNullable(store.get(subjectId));
        }

        void put(Subject subject) {
            store.put(subject.getSubjectId(), subject);
        }

        @Override
        public Subject save(Subject subject) {
            if (store.containsKey(subject.getSubjectId())) {
                throw ConditionalCheckFailedException.builder().message("exists").build();
            }
            store.put(subject.getSubjectId(), subject);
            return subject;
        }

        @Override
        public Subject update(Subject subject) {
            if (!store.containsKey(subject.getSubjectId())) {
                throw ConditionalCheckFailedException.builder().message("not found").build();
            }
            store.put(subject.getSubjectId(), subject);
            return subject;
        }
    }
}
