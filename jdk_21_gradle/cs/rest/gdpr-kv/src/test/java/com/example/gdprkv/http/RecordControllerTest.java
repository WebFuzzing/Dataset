package com.example.gdprkv.http;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.gdprkv.models.Record;
import com.example.gdprkv.requests.DeleteRecordServiceRequest;
import com.example.gdprkv.requests.PutRecordServiceRequest;
import com.example.gdprkv.service.AuditLogService;
import com.example.gdprkv.service.GdprKvException;
import com.example.gdprkv.service.PolicyDrivenRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(controllers = RecordController.class)
@Import(RequestIdFilter.class)
class RecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PolicyDrivenRecordService recordService;

    @MockBean
    private AuditLogService auditLogService;

    @Test
    @DisplayName("PUT record returns 200 and response body")
    void putRecordSuccess() throws Exception {
        when(recordService.putRecord(any())).thenAnswer(invocation -> {
            PutRecordServiceRequest req = invocation.getArgument(0);
            return Record.builder()
                    .subjectId(req.subjectId())
                    .recordKey(req.recordKey())
                    .purpose(req.purpose())
                    .value(objectMapper.readTree("{\"email\":\"demo@example.com\"}"))
                    .createdAt(Instant.parse("2024-09-01T10:00:00Z").toEpochMilli())
                    .updatedAt(Instant.parse("2024-09-01T10:05:00Z").toEpochMilli())
                    .version(2L)
                    .retentionDays(30)
                    .tombstoned(false)
                    .requestId(req.requestId())
                    .build();
        });

        String body = "{" +
                "\"purpose\":\"FULFILLMENT\"," +
                "\"value\":{\"email\":\"demo@example.com\"}" +
                "}";

        mockMvc.perform(MockMvcRequestBuilders.put("/subjects/sub_123/records/pref:email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.header().exists("X-Request-Id"))
                .andExpect(MockMvcResultMatchers.header().string("ETag", equalTo("2")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject_id", equalTo("sub_123")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.record_key", equalTo("pref:email")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.purpose", equalTo("FULFILLMENT")))
                .andReturn();

        ArgumentCaptor<PutRecordServiceRequest> captor = ArgumentCaptor.forClass(PutRecordServiceRequest.class);
        verify(recordService, times(1)).putRecord(captor.capture());
        PutRecordServiceRequest writeRequest = captor.getValue();
        assertEquals("sub_123", writeRequest.subjectId());
        assertEquals("pref:email", writeRequest.recordKey());
        assertEquals("FULFILLMENT", writeRequest.purpose());
        assertFalse(writeRequest.requestId().isBlank());

        verify(auditLogService).recordPutRequested("sub_123", "pref:email", "FULFILLMENT", writeRequest.requestId());
        verify(auditLogService).recordPutSuccess(any(Record.class));

    }

    @Test
    @DisplayName("PUT record returns 400 when invalid purpose")
    void putRecordInvalidPurpose() throws Exception {
        when(recordService.putRecord(any())).thenThrow(GdprKvException.invalidPurpose("UNKNOWN"));

        String body = "{" +
                "\"purpose\":\"UNKNOWN\"," +
                "\"value\":{}" +
                "}";

        mockMvc.perform(MockMvcRequestBuilders.put("/subjects/sub_999/records/pref:email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code", equalTo("INVALID_PURPOSE")));

        verify(auditLogService).recordPutRequested(any(), any(), any(), any());
        verify(auditLogService).recordPutFailure(any(), any(), any(), any(), any());
        verify(auditLogService, never()).recordPutSuccess(any());
    }

    @Test
    @DisplayName("PUT record returns 404 when subject missing")
    void putRecordSubjectMissing() throws Exception {
        when(recordService.putRecord(any())).thenThrow(GdprKvException.subjectNotFound("ghost"));

        String body = "{" +
                "\"purpose\":\"FULFILLMENT\"," +
                "\"value\":{}" +
                "}";

        mockMvc.perform(MockMvcRequestBuilders.put("/subjects/ghost/records/pref:email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code", equalTo("SUBJECT_NOT_FOUND")));

        verify(auditLogService).recordPutRequested(any(), any(), any(), any());
        verify(auditLogService).recordPutFailure(any(), any(), any(), any(), any());
        verify(auditLogService, never()).recordPutSuccess(any());
    }

    @Test
    @DisplayName("PUT record emits filter-generated request id when none supplied")
    void putRecordGeneratesRequestId() throws Exception {
        ArgumentCaptor<PutRecordServiceRequest> captor = ArgumentCaptor.forClass(PutRecordServiceRequest.class);

        when(recordService.putRecord(any())).thenAnswer(invocation -> {
            PutRecordServiceRequest req = invocation.getArgument(0);
            return Record.builder()
                    .subjectId(req.subjectId())
                    .recordKey(req.recordKey())
                    .purpose(req.purpose())
                    .requestId(req.requestId())
                    .createdAt(1L)
                    .updatedAt(1L)
                    .version(1L)
                    .retentionDays(30)
                    .tombstoned(false)
                    .build();
        });

        String body = "{" +
                "\"purpose\":\"FULFILLMENT\"," +
                "\"value\":{}" +
                "}";

        var result = mockMvc.perform(MockMvcRequestBuilders.put("/subjects/sub_abc/records/pref:sms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        verify(recordService).putRecord(captor.capture());
        PutRecordServiceRequest writeRequest = captor.getValue();
        assertEquals("sub_abc", writeRequest.subjectId());
        assertFalse(writeRequest.requestId().isBlank());

        verify(auditLogService).recordPutRequested("sub_abc", "pref:sms", "FULFILLMENT", writeRequest.requestId());
        verify(auditLogService).recordPutSuccess(any(Record.class));

        String headerId = result.getResponse().getHeader("X-Request-Id");
        assertFalse(headerId == null || headerId.isBlank());
    }

    @Test
    @DisplayName("DELETE record returns 200 and tombstoned record")
    void deleteRecordSuccess() throws Exception {
        long now = Instant.parse("2024-09-01T10:00:00Z").toEpochMilli();
        long purgeDueAt = now + (86400000L * 30); // 30 days later

        when(recordService.deleteRecord(any())).thenAnswer(invocation -> {
            DeleteRecordServiceRequest req = invocation.getArgument(0);
            return Record.builder()
                    .subjectId(req.subjectId())
                    .recordKey(req.recordKey())
                    .purpose("FULFILLMENT")
                    .value(objectMapper.readTree("{\"email\":\"demo@example.com\"}"))
                    .createdAt(now - 1000000)
                    .updatedAt(now)
                    .version(2L)
                    .retentionDays(30)
                    .tombstoned(true)
                    .tombstonedAt(now)
                    .purgeDueAt(purgeDueAt)
                    .purgeBucket("2024-10-01T10")
                    .requestId(req.requestId())
                    .build();
        });

        mockMvc.perform(MockMvcRequestBuilders.delete("/subjects/sub_123/records/pref:email"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.header().exists("X-Request-Id"))
                .andExpect(MockMvcResultMatchers.header().string("ETag", equalTo("2")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject_id", equalTo("sub_123")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.record_key", equalTo("pref:email")))
                .andReturn();

        ArgumentCaptor<DeleteRecordServiceRequest> captor = ArgumentCaptor.forClass(DeleteRecordServiceRequest.class);
        verify(recordService, times(1)).deleteRecord(captor.capture());
        DeleteRecordServiceRequest deleteRequest = captor.getValue();
        assertEquals("sub_123", deleteRequest.subjectId());
        assertEquals("pref:email", deleteRequest.recordKey());
        assertFalse(deleteRequest.requestId().isBlank());

        verify(auditLogService).recordDeleteRequested("sub_123", "pref:email", deleteRequest.requestId());
        verify(auditLogService).recordDeleteSuccess(any(Record.class));
        verify(auditLogService, never()).recordDeleteAlreadyTombstoned(any(), any(), any());
    }

    @Test
    @DisplayName("DELETE record returns 404 when record not found")
    void deleteRecordNotFound() throws Exception {
        when(recordService.deleteRecord(any())).thenThrow(GdprKvException.recordNotFound("sub_123", "pref:email"));

        mockMvc.perform(MockMvcRequestBuilders.delete("/subjects/sub_123/records/pref:email"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code", equalTo("RECORD_NOT_FOUND")));

        verify(auditLogService).recordDeleteRequested(any(), any(), any());
        verify(auditLogService).recordDeleteFailure(any(), any(), any(), any());
        verify(auditLogService, never()).recordDeleteSuccess(any());
        verify(auditLogService, never()).recordDeleteAlreadyTombstoned(any(), any(), any());
    }

    @Test
    @DisplayName("DELETE record returns 404 when subject not found")
    void deleteRecordSubjectNotFound() throws Exception {
        when(recordService.deleteRecord(any())).thenThrow(GdprKvException.subjectNotFound("ghost"));

        mockMvc.perform(MockMvcRequestBuilders.delete("/subjects/ghost/records/pref:email"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code", equalTo("SUBJECT_NOT_FOUND")));

        verify(auditLogService).recordDeleteRequested(any(), any(), any());
        verify(auditLogService).recordDeleteFailure(any(), any(), any(), any());
        verify(auditLogService, never()).recordDeleteSuccess(any());
    }
}
