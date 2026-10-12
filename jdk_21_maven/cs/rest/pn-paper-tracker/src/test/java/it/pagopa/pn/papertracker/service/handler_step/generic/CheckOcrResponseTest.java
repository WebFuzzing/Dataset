package it.pagopa.pn.papertracker.service.handler_step.generic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sngular.apigenerator.asyncapi.business_model.model.event.Data;
import com.sngular.apigenerator.asyncapi.business_model.model.event.OcrDataResultPayload;
import it.pagopa.pn.papertracker.exception.PnPaperTrackerValidationException;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.model.OcrStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckOcrResponseTest {

    @Mock
    private PaperTrackingsDAO paperTrackingsDAO;

    private CheckOcrResponse checkOcrResponse;

    private HandlerContext context;
    private PaperTrackings paperTrackings;

    @BeforeEach
    void setUp() {
        context = new HandlerContext();
        paperTrackings = new PaperTrackings();
        paperTrackings.setTrackingId("trackingId-1");
        ValidationConfig config = new ValidationConfig();
        config.setOcrEnabled(OcrStatusEnum.RUN);
        paperTrackings.setValidationConfig(config);
        Event event = new Event();
        event.setDryRun(false);
        event.setId("event-1");
        paperTrackings.setState(PaperTrackingsState.AWAITING_OCR);
        paperTrackings.setEvents(List.of(event));
        ValidationFlow validationFlow = new ValidationFlow();
        validationFlow.setOcrRequests(List.of());
        paperTrackings.setValidationFlow(validationFlow);
        context.setPaperTrackings(paperTrackings);
        OcrDataResultPayload ocrPayload = OcrDataResultPayload.builder()
                .commandId("trackingId-1#event-1#AR")
                .data(Data.builder().validationStatus(Data.ValidationStatus.OK).build())
                .build();
        context.setOcrDataResultPayload(ocrPayload);
        checkOcrResponse = new CheckOcrResponse(paperTrackingsDAO, new ObjectMapper());
    }

    @Test
    void execute_invalidState_ocrRUN() {
        paperTrackings.setState(PaperTrackingsState.DONE);
        StepVerifier.create(checkOcrResponse.execute(context))
                .expectError(PnPaperTrackerValidationException.class)
                .verify();
    }

    @Test
    void execute_invalidState_ocrDRY_ocrValidationStatusOK() {
        paperTrackings.setState(PaperTrackingsState.DONE);
        paperTrackings.getValidationConfig().setOcrEnabled(OcrStatusEnum.DRY);
        when(paperTrackingsDAO.updateOcrRequests(any(), any(), eq(Data.ValidationStatus.OK))).thenReturn(Mono.just(paperTrackings));
        StepVerifier.create(checkOcrResponse.execute(context))
                .verifyComplete();
        assertTrue(context.isStopExecution());
    }

    @Test
    void execute_invalidState_ocrDRY_ocrValidationStatusKO() {
        paperTrackings.setState(PaperTrackingsState.DONE);
        paperTrackings.getValidationConfig().setOcrEnabled(OcrStatusEnum.DRY);
        context.getOcrDataResultPayload().setData(Data.builder().validationStatus(Data.ValidationStatus.KO).build());
        when(paperTrackingsDAO.updateOcrRequests(any(), any(), eq(Data.ValidationStatus.KO))).thenReturn(Mono.just(paperTrackings));
        StepVerifier.create(checkOcrResponse.execute(context))
                .expectError(PnPaperTrackerValidationException.class)
                .verify();
    }

    @Test
    void execute_ocrRUN_ocrValidationStatusKO() {
        context.getOcrDataResultPayload().setData(Data.builder().validationStatus(Data.ValidationStatus.KO).build());
        when(paperTrackingsDAO.updateOcrRequests(any(), any(), eq(Data.ValidationStatus.KO))).thenReturn(Mono.just(paperTrackings));
        StepVerifier.create(checkOcrResponse.execute(context))
                .expectError(PnPaperTrackerValidationException.class)
                .verify();
    }

    @Test
    void execute_ocrRUN_ocrValidationStatusOK() {
        when(paperTrackingsDAO.updateOcrRequests(any(), any(), eq(Data.ValidationStatus.OK))).thenReturn(Mono.just(paperTrackings));
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(paperTrackings));
        StepVerifier.create(checkOcrResponse.execute(context))
                .verifyComplete();
        assertFalse(context.isStopExecution());
    }

    @Test
    void execute_ocrRUN_ocrValidationStatusPENDING() {
        when(paperTrackingsDAO.updateOcrRequests(any(), any(), eq(Data.ValidationStatus.PENDING))).thenReturn(Mono.just(paperTrackings));
        context.getOcrDataResultPayload().setData(Data.builder().validationStatus(Data.ValidationStatus.PENDING).build());
        StepVerifier.create(checkOcrResponse.execute(context))
                .verifyComplete();
        assertTrue(context.isStopExecution());
    }

}