package it.pagopa.pn.papertracker.utils;

import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.PcRetryResponse;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.CON996;
import static org.junit.jupiter.api.Assertions.*;

class PcRetryUtilsMockTest {

    private static PaperTrackings tracking(String requestId, String driverId) {
        PaperTrackings pt = new PaperTrackings();
        pt.setTrackingId(requestId);
        pt.setUnifiedDeliveryDriver(driverId);
        return pt;
    }


    @Test
    void returnsZero_whenNoSuffixPresent() {
        assertEquals(0, PcRetryUtilsMock.getRetryAttempt("REQ-123"));
    }

    @Test
    void returnsZero_whenSuffixIsZero() {
        assertEquals(0, PcRetryUtilsMock.getRetryAttempt("REQ.PCRETRY_0"));
    }

    @Test
    void returnsNumber_whenSuffixIsMultiDigit() {
        assertEquals(1, PcRetryUtilsMock.getRetryAttempt("REQ.PCRETRY_1"));
    }

    @Test
    void returnsCorrect_whenOtherUnderscoresExist() {
        assertEquals(3, PcRetryUtilsMock.getRetryAttempt("A_B_C.PCRETRY_3"));
    }


    @Test
    void increments_whenSuffixPresent() {
        assertEquals("REQ.PCRETRY_1", PcRetryUtilsMock.setRetryRequestId("REQ.PCRETRY_0"));
        assertEquals("REQ.PCRETRY_2", PcRetryUtilsMock.setRetryRequestId("REQ.PCRETRY_1"));
    }

    @Test
    void unchanged_whenSuffixMissing() {
        assertEquals("REQ-123", PcRetryUtilsMock.setRetryRequestId("REQ-123"));
    }


    @Test
    void true_whenUnlimitedMinusOne() {
        assertTrue(PcRetryUtilsMock.hasOtherAttempt("REQ.PCRETRY_999", -1));
    }

    @Test
    void true_whenMaxRetryEqualsCurrentAttempt() {
        assertTrue(PcRetryUtilsMock.hasOtherAttempt("REQ.PCRETRY_3", 3));
    }

    @Test
    void false_whenMaxRetryEqualsCurrentAttempt() {
        assertFalse(PcRetryUtilsMock.hasOtherAttempt("REQ.PCRETRY_4", 3));
    }

    @Test
    void true_whenMaxRetryLessThanCurrentAttempt() {
        assertTrue(PcRetryUtilsMock.hasOtherAttempt("REQ.PCRETRY_0", 0));
    }

    @Test
    void false_whenMaxRetryLessThanCurrentAttempt() {
        assertFalse(PcRetryUtilsMock.hasOtherAttempt("REQ.PCRETRY_1", 0));
    }


    @Test
    void setsBothFields_whenSuffixPresent() {
        PcRetryResponse resp = new PcRetryResponse();
        PcRetryUtilsMock.setRetryRequestIdAndPcRetry(resp, "REQ.PCRETRY_7");

        assertEquals("REQ.PCRETRY_7", resp.getRequestId());
        assertEquals("PCRETRY_7", resp.getPcRetry());
    }


    @Test
    void retryFound_nonCon996_andWithinLimit_incrementsSuffixAndSetsFields() {
        PaperTrackings pt = tracking("REQ.PCRETRY_3", "DRV-1");

        StepVerifier.create(PcRetryUtilsMock.getPcRetryPaperMock(pt, 3, "RECRN006"))
                .assertNext(resp -> {
                    assertEquals(Boolean.TRUE, resp.getRetryFound());
                    assertEquals(pt.getTrackingId(), resp.getParentRequestId());
                    assertEquals(pt.getUnifiedDeliveryDriver(), resp.getDeliveryDriverId());
                    assertEquals("REQ.PCRETRY_4", resp.getRequestId());
                    assertEquals("PCRETRY_4", resp.getPcRetry());
                })
                .verifyComplete();
    }

    @Test
    void noRetryFound_nonCon996_whenOverLimit() {
        PaperTrackings pt = tracking("REQ.PCRETRY_4", "DRV-1");

        StepVerifier.create(PcRetryUtilsMock.getPcRetryPaperMock(pt, 3, "RECRN006"))
                .assertNext(resp -> {
                    assertNotEquals(Boolean.TRUE, resp.getRetryFound());
                    assertEquals(pt.getTrackingId(), resp.getParentRequestId());
                    assertEquals(pt.getUnifiedDeliveryDriver(), resp.getDeliveryDriverId());
                    assertNull(resp.getRequestId());
                    assertNull(resp.getPcRetry());
                })
                .verifyComplete();
    }

    @Test
    void retryFound_CON996_allowsOnlyIfCurrentAttemptIsZero() {
        PaperTrackings pt = tracking("REQ.PCRETRY_0", "DRV-1");

        StepVerifier.create(PcRetryUtilsMock.getPcRetryPaperMock(pt, 0, CON996.name()))
                .assertNext(resp -> {
                    assertEquals(Boolean.TRUE, resp.getRetryFound());
                    assertEquals("REQ.PCRETRY_1", resp.getRequestId());
                    assertEquals("PCRETRY_1", resp.getPcRetry());
                })
                .verifyComplete();
    }

    @Test
    void noRetryFound_CON996_whenCurrentAttemptGreaterThanZero() {
        PaperTrackings pt = tracking("REQ.PCRETRY_1", "DRV-1");

        StepVerifier.create(PcRetryUtilsMock.getPcRetryPaperMock(pt, 0, CON996.name()))
                .assertNext(resp -> {
                    assertNotEquals(Boolean.TRUE, resp.getRetryFound());
                    assertNull(resp.getRequestId());
                    assertNull(resp.getPcRetry());
                })
                .verifyComplete();
    }

    @Test
    void noRetryFound_whenSuffixMissing_andRetryNotAllowed() {
        PaperTrackings pt = tracking("REQ.PCRETRY_0", "DRV-1");

        StepVerifier.create(PcRetryUtilsMock.getPcRetryPaperMock(pt, -1, "RECRN006"))
                .assertNext(resp -> {
                    assertEquals(Boolean.TRUE, resp.getRetryFound());
                    assertEquals("REQ.PCRETRY_1", resp.getRequestId());
                    assertEquals("PCRETRY_1", resp.getPcRetry());
                })
                .verifyComplete();
    }
}
