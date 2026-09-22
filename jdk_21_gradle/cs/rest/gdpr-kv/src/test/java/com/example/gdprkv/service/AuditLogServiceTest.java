package com.example.gdprkv.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.gdprkv.access.AuditEventAccess;
import com.example.gdprkv.models.AuditEvent;
import com.example.gdprkv.models.Record;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class AuditLogServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2024-10-01T12:34:56Z"), ZoneOffset.UTC);

    private AuditEventAccess access;
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        access = Mockito.mock(AuditEventAccess.class);
        auditLogService = new AuditLogService(access, CLOCK);
    }

    @Test
    @DisplayName("recordPutRequested appends PUT_REQUESTED event")
    void recordPutRequested() {
        when(access.findLatest("sub")).thenReturn(Optional.empty());

        auditLogService.recordPutRequested("sub", "pref:email", "PURPOSE", "req-1");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(access).put(captor.capture());
        AuditEvent event = captor.getValue();
        assertEquals(AuditEvent.EventType.PUT_REQUESTED, event.getEventType());
        assertEquals("sub", event.getSubjectId());
        assertEquals("pref:email", event.getItemKey());
        assertEquals("PURPOSE", event.getPurpose());
        assertEquals("req-1", event.getRequestId());
        assertEquals(Instant.now(CLOCK).toEpochMilli(), event.getTimestamp());
        assertEquals("0".repeat(64), event.getPrevHash());
        assertNotNull(event.getHash());
    }

    @Test
    @DisplayName("recordPutSuccess chooses new vs update events")
    void recordPutSuccess() {
        when(access.findLatest("sub")).thenReturn(Optional.of(sampleEvent(AuditEvent.EventType.PUT_REQUESTED)));

        Record newRecord = Record.builder()
                .subjectId("sub")
                .recordKey("k")
                .purpose("P")
                .requestId("req")
                .createdAt(Instant.now(CLOCK).toEpochMilli())
                .updatedAt(Instant.now(CLOCK).toEpochMilli())
                .retentionDays(30)
                .version(1L)
                .build();

        auditLogService.recordPutSuccess(newRecord);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(access).put(captor.capture());
        assertEquals(AuditEvent.EventType.PUT_NEW_ITEM_SUCCESS, captor.getValue().getEventType());

        Record existing = newRecord.toBuilder().version(5L).build();
        when(access.findLatest("sub")).thenReturn(Optional.of(captor.getValue()));

        auditLogService.recordPutSuccess(existing);
        verify(access, times(2)).put(captor.capture());
        assertEquals(AuditEvent.EventType.PUT_UPDATE_ITEM_SUCCESS,
                captor.getValue().getEventType());
    }

    @Test
    @DisplayName("recordPutFailure appends failure event with error details")
    void recordPutFailure() {
        when(access.findLatest("sub")).thenReturn(Optional.of(sampleEvent(AuditEvent.EventType.PUT_REQUESTED)));

        auditLogService.recordPutFailure("sub", "k", "P", "req", "boom");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(access).put(captor.capture());
        AuditEvent event = captor.getValue();
        assertEquals(AuditEvent.EventType.PUT_FAILED, event.getEventType());
        assertEquals("boom", event.getDetails().get("error"));
    }

    private AuditEvent sampleEvent(AuditEvent.EventType type) {
        return AuditEvent.builder()
                .subjectId("sub")
                .tsUlid("0_MOCK")
                .eventType(type)
                .requestId("req")
                .timestamp(Instant.now(CLOCK).toEpochMilli())
                .prevHash("0".repeat(64))
                .build();
    }
}
