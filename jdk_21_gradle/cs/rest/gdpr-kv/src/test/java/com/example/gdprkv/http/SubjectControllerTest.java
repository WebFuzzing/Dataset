package com.example.gdprkv.http;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.gdprkv.models.Subject;
import com.example.gdprkv.requests.DeleteSubjectServiceRequest;
import com.example.gdprkv.requests.PutSubjectServiceRequest;
import com.example.gdprkv.service.AuditLogService;
import com.example.gdprkv.service.GdprKvException;
import com.example.gdprkv.service.SubjectService;
import com.example.gdprkv.service.SubjectService.SubjectDeletionResult;
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

@WebMvcTest(controllers = SubjectController.class)
@Import(RequestIdFilter.class)
class SubjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubjectService subjectService;

    @MockBean
    private AuditLogService auditLogService;

    @Test
    @DisplayName("PUT subject creates subject metadata")
    void putSubject() throws Exception {
        when(subjectService.putSubject(any())).thenReturn(Subject.builder()
                .subjectId("demo")
                .createdAt(1700000000000L)
                .residency("US")
                .requestId("test-req-123")
                .build());

        mockMvc.perform(MockMvcRequestBuilders.put("/subjects/demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"residency\":\"US\"}"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject_id", equalTo("demo")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.residency", equalTo("US")));

        ArgumentCaptor<PutSubjectServiceRequest> captor = ArgumentCaptor.forClass(PutSubjectServiceRequest.class);
        verify(subjectService).putSubject(captor.capture());
        PutSubjectServiceRequest request = captor.getValue();
        assertEquals("demo", request.subjectId());
        assertEquals("US", request.residency());
    }

    @Test
    @DisplayName("PUT subject returns 409 when subject exists")
    void putSubjectConflict() throws Exception {
        when(subjectService.putSubject(any())).thenThrow(GdprKvException.subjectAlreadyExists("demo"));

        mockMvc.perform(MockMvcRequestBuilders.put("/subjects/demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"residency\":\"US\"}"))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code", equalTo("SUBJECT_ALREADY_EXISTS")));
    }

    @Test
    @DisplayName("DELETE subject marks for erasure and deletes all records")
    void deleteSubjectSuccess() throws Exception {
        long now = System.currentTimeMillis();
        Subject deletedSubject = Subject.builder()
                .subjectId("demo")
                .createdAt(now - 100000)
                .residency("US")
                .erasureInProgress(true)
                .erasureRequestedAt(now)
                .requestId("test-req-456")
                .build();

        SubjectDeletionResult result = new SubjectDeletionResult(deletedSubject, 3, 3);
        when(subjectService.deleteSubject(any())).thenReturn(result);

        mockMvc.perform(MockMvcRequestBuilders.delete("/subjects/demo"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.header().exists("X-Request-Id"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject.subject_id", equalTo("demo")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subject.erasure_in_progress", equalTo(true)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.records_deleted", equalTo(3)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.total_records", equalTo(3)));

        ArgumentCaptor<DeleteSubjectServiceRequest> captor = ArgumentCaptor.forClass(DeleteSubjectServiceRequest.class);
        verify(subjectService).deleteSubject(captor.capture());
        assertEquals("demo", captor.getValue().subjectId());

        verify(auditLogService).recordSubjectErasureRequested(anyString(), anyString());
        verify(auditLogService).recordSubjectErasureStarted(anyString(), anyString(), anyInt());
        verify(auditLogService).recordSubjectErasureCompleted(anyString(), anyString(), anyInt());
        verify(auditLogService, never()).recordSubjectErasureFailure(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("DELETE subject returns 404 when subject not found")
    void deleteSubjectNotFound() throws Exception {
        when(subjectService.deleteSubject(any())).thenThrow(GdprKvException.subjectNotFound("ghost"));

        mockMvc.perform(MockMvcRequestBuilders.delete("/subjects/ghost"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code", equalTo("SUBJECT_NOT_FOUND")));

        verify(auditLogService).recordSubjectErasureRequested(anyString(), anyString());
        verify(auditLogService).recordSubjectErasureFailure(anyString(), anyString(), anyString());
        verify(auditLogService, never()).recordSubjectErasureStarted(anyString(), anyString(), anyInt());
        verify(auditLogService, never()).recordSubjectErasureCompleted(anyString(), anyString(), anyInt());
    }
}
