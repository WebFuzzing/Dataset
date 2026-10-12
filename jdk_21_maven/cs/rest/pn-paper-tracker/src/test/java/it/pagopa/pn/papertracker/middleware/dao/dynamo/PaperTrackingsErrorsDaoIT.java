package it.pagopa.pn.papertracker.middleware.dao.dynamo;

import it.pagopa.pn.papertracker.BaseTest;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsErrorsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class PaperTrackingsErrorsDaoIT extends BaseTest.WithLocalStack {

    @Autowired
    PaperTrackingsErrorsDAO paperTrackingsErrorsDAO;

    @Test
    void insertAndRetrieveError() {
        //Arrange
        IntStream.range(0, 3).forEach(i -> {
            PaperTrackingsErrors error = new PaperTrackingsErrors();
            error.setTrackingId("requestId1");
            error.setCreated(Instant.now().minus(i, ChronoUnit.MINUTES));
            error.setErrorCategory(ErrorCategory.OCR_VALIDATION);
            error.setFlowThrow(FlowThrow.DEMAT_VALIDATION);
            error.setProductType(ProductType.AR.getValue());
            error.setType(ErrorType.WARNING);
            ErrorDetails errorDetails = new ErrorDetails();
            errorDetails.setCause(ErrorCause.OCR_KO);
            errorDetails.setMessage("error");
            error.setDetails(errorDetails);
            error.setEventThrow("RECRN001C");

            paperTrackingsErrorsDAO.insertError(error).block();
        });

        PaperTrackingsErrors error = new PaperTrackingsErrors();
        error.setTrackingId("requestId2");
        error.setCreated(Instant.now());
        error.setErrorCategory(ErrorCategory.OCR_VALIDATION);
        error.setFlowThrow(FlowThrow.DEMAT_VALIDATION);
        error.setProductType(ProductType.AR.getValue());
        error.setType(ErrorType.ERROR);
        ErrorDetails errorDetails = new ErrorDetails();
        errorDetails.setCause(ErrorCause.GIACENZA_DATE_ERROR);
        errorDetails.setMessage("error");
        error.setDetails(errorDetails);
        error.setEventThrow("RECRN001C");

        paperTrackingsErrorsDAO.insertError(error).block();

        List<PaperTrackingsErrors> errors = paperTrackingsErrorsDAO.retrieveErrors("requestId1").collectList().block();

        //Assert
        Assertions.assertNotNull(errors);
        Assertions.assertEquals(3, errors.size());
        Assertions.assertTrue(errors.stream().allMatch(e -> e.getTrackingId().equals("requestId1")));
        Assertions.assertTrue(errors.stream().allMatch(e -> e.getErrorCategory() == ErrorCategory.OCR_VALIDATION));
        Assertions.assertTrue(errors.stream().allMatch(e -> e.getFlowThrow() == FlowThrow.DEMAT_VALIDATION));
        Assertions.assertTrue(errors.stream().allMatch(e -> Objects.equals(e.getProductType(), ProductType.AR.getValue())));
        Assertions.assertTrue(errors.stream().allMatch(e -> e.getDetails().getCause() == ErrorCause.OCR_KO));
        Assertions.assertTrue(errors.stream().allMatch(e -> e.getDetails().getMessage().equals("error")));
        Assertions.assertTrue(errors.stream().allMatch(e -> e.getEventThrow().equals("RECRN001C")));
        Assertions.assertTrue(errors.stream().allMatch(e -> e.getType() == ErrorType.WARNING));

        List<PaperTrackingsErrors> errors2 = paperTrackingsErrorsDAO.retrieveErrors("requestId2").collectList().block();
        Assertions.assertNotNull(errors2);
        Assertions.assertEquals(1, errors2.size());
        Assertions.assertEquals("requestId2", errors2.getFirst().getTrackingId());
        Assertions.assertSame(ErrorCategory.OCR_VALIDATION, errors2.getFirst().getErrorCategory());
        Assertions.assertSame(FlowThrow.DEMAT_VALIDATION, errors2.getFirst().getFlowThrow());
        Assertions.assertEquals(ProductType.AR.getValue(), errors2.getFirst().getProductType());
        Assertions.assertEquals(ErrorCause.GIACENZA_DATE_ERROR, errors2.getFirst().getDetails().getCause());
        Assertions.assertEquals("error", errors2.getFirst().getDetails().getMessage());
        Assertions.assertEquals("RECRN001C", errors2.getFirst().getEventThrow());
        Assertions.assertSame(ErrorType.ERROR, errors2.getFirst().getType());

    }
}
