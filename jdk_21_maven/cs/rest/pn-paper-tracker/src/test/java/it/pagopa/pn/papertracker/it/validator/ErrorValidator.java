package it.pagopa.pn.papertracker.it.validator;

import it.pagopa.pn.papertracker.it.model.ProductTestCase;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackingsErrors;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class ErrorValidator {

    public static void verifyErrors(ProductTestCase scenario,
                                    List<PaperTrackingsErrors> actualErrors) {

        List<PaperTrackingsErrors> expected = scenario.getExpected().getErrors();
        if (expected == null) return;

        assertEquals(expected.size(), actualErrors.size(), "Mismatch error count");

        for (PaperTrackingsErrors exp : expected) {

            PaperTrackingsErrors match = actualErrors.stream()
                    .filter(act -> sameErrorIdentity(exp, act))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "Expected error not found: " + exp));

            verifyErrorFields(exp, match);

            assertNotNull(match.getCreated());
        }
    }

    private static void verifyErrorFields(PaperTrackingsErrors exp,
                                          PaperTrackingsErrors act) {

        assertAll("Error fields",
                () -> assertEquals(exp.getTrackingId(), act.getTrackingId()),
                () -> assertEquals(exp.getErrorCategory(), act.getErrorCategory()),
                () -> assertEquals(exp.getFlowThrow(), act.getFlowThrow()),
                () -> assertEquals(exp.getType(), act.getType()),
                () -> assertEquals(exp.getProductType(), act.getProductType()),
                () -> assertEquals(exp.getEventThrow(), act.getEventThrow()),
                () -> assertEquals(exp.getEventIdThrow(), act.getEventIdThrow())
        );
    }

    private static boolean sameErrorIdentity(PaperTrackingsErrors exp,
                                             PaperTrackingsErrors act) {

        return Objects.equals(exp.getTrackingId(), act.getTrackingId())
                && exp.getErrorCategory() == act.getErrorCategory()
                && exp.getFlowThrow() == act.getFlowThrow()
                && exp.getType() == act.getType()
                && Objects.equals(exp.getProductType(), act.getProductType())
                && Objects.equals(exp.getEventThrow(), act.getEventThrow())
                && Objects.equals(exp.getEventIdThrow(), act.getEventIdThrow())
                && act.getDetails().getMessage().contains(exp.getDetails().getMessage())
                && (Objects.isNull(act.getDetails().getCause()) || Objects.equals(exp.getDetails().getCause(), act.getDetails().getCause()))
                && checkAdditionalDetails(act, exp);
    }

    private static boolean checkAdditionalDetails(PaperTrackingsErrors act, PaperTrackingsErrors exp) {
        if( Objects.isNull(act.getDetails().getAdditionalDetails()) ){
            return true;
        }
        if(act.getDetails().getAdditionalDetails().containsKey("affectedEvents")){
           return Objects.equals(exp.getDetails().getAdditionalDetails().get("affectedEvents"), act.getDetails().getAdditionalDetails().get("affectedEvents"));
        }
        return Objects.equals(exp.getDetails().getAdditionalDetails(), act.getDetails().getAdditionalDetails());
    }
}
