package it.pagopa.pn.papertracker.it.validator;

import it.pagopa.pn.papertracker.it.model.ProductTestCase;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.Attachment;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackerDryRunOutputs;
import it.pagopa.pn.papertracker.model.OcrStatusEnum;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@Component
public class OutputValidator {

    private static final Map<String, List<SwapRule>> swapRules = new HashMap<>();
    private static final Map<String, ReorderRule> reorderRules = new HashMap<>();

    public OutputValidator() {

        // -------- SWAP RULES --------
        swapRules.put("OK_GIACENZA_GT10_890", List.of(new SwapRule("RECAG005A", "RECAG012")));
        swapRules.put("OK_GIACENZA_DELEGATO_GT10_890", List.of(new SwapRule("RECAG006A", "RECAG012")));
        swapRules.put("OK_COMPIUTA_GIACENZA_890", List.of(new SwapRule("RECAG008A", "RECAG012"), new SwapRule("RECAG008A", "RECAG008B")));
        swapRules.put("OK_COMPIUTA_GIACENZA_890_RECAG010A", List.of(new SwapRule("RECAG008A", "RECAG012"), new SwapRule("RECAG008A", "RECAG008B")));
        swapRules.put("OK_COMPIUTA_GIACENZA_INVALID_ATTACHMENT_890", List.of(new SwapRule("RECAG008A", "RECAG012"), new SwapRule("RECAG008A", "RECAG008B")));
        swapRules.put("OK_COMPIUTA_GIACENZA_NO_ATTACHMENT_890", List.of(new SwapRule("RECAG008A", "RECAG012")));
        swapRules.put("FAIL_GIACENZA_NO_ATTACHMENT_890", List.of(new SwapRule("RECAG007A", "RECAG012")));
        swapRules.put("FAIL_GIACENZA_INVALID_ATTACHMENT_890", List.of(new SwapRule("RECAG007A", "RECAG012"), new SwapRule("RECAG007A", "RECAG007B")));
        swapRules.put("FAIL_GIACENZA_GT10_890", List.of(new SwapRule("RECAG007A", "RECAG012"), new SwapRule("RECAG007B", "RECAG007A")));
        swapRules.put("OK_GIACENZA_DELEGATO_NO_ATTACHMENT_890", List.of(new SwapRule("RECAG005A", "RECAG012"), new SwapRule("RECAG005A", "RECAG005B")));

        // -------- REORDER RULE (scenario con 2 eventi B) --------
        //FIX
        reorderRules.put("FAIL_GIACENZA_LTE10_890", new ReorderRule("RECAG012", 5));
    }


    public static void verifyOutputs(ProductTestCase scenario,
                                     OcrStatusEnum ocrStatusEnum,
                                     List<PaperTrackerDryRunOutputs> actualOutputs) {

        List<PaperTrackerDryRunOutputs> expected = scenario.getExpected().getOutputs();
        if (expected == null) return;

        assertEquals(expected.size(), actualOutputs.size(), "Mismatch output count");

        List<PaperTrackerDryRunOutputs> sortedActual =
                sortIfNeeded(scenario, ocrStatusEnum, actualOutputs);

        for (int i = 0; i < expected.size(); i++) {

            PaperTrackerDryRunOutputs exp = expected.get(i);
            PaperTrackerDryRunOutputs act = sortedActual.get(i);

            assertAll("Output at position " + i,
                    () -> assertNotNull(act.getCreated()),
                    () -> assertEquals(exp.getTrackingId(), act.getTrackingId()),
                    () -> assertEquals(exp.getRegisteredLetterCode(), act.getRegisteredLetterCode()),
                    () -> assertEquals(exp.getStatusCode(), act.getStatusCode()),
                    () -> assertEquals(exp.getStatusDetail(), act.getStatusDetail()),
                    () -> assertEquals(exp.getStatusDescription(), act.getStatusDescription()),
                    () -> assertEquals(
                            OffsetDateTime.parse(exp.getStatusDateTime()),
                            OffsetDateTime.parse(act.getStatusDateTime())
                    ),
                    () -> assertEquals(exp.getDeliveryFailureCause(), act.getDeliveryFailureCause()),
                    () -> assertEquals(exp.getAnonymizedDiscoveredAddressId(),
                            act.getAnonymizedDiscoveredAddressId()),
                    () -> assertEquals(
                            OffsetDateTime.parse(exp.getClientRequestTimestamp()),
                            OffsetDateTime.parse(act.getClientRequestTimestamp())
                    )
            );

            verifyOutputAttachments(exp.getAttachments(), act.getAttachments(), i);
        }
    }

    private static List<PaperTrackerDryRunOutputs> sortIfNeeded(
            ProductTestCase scenario,
            OcrStatusEnum ocrStatusEnum,
            List<PaperTrackerDryRunOutputs> actualOutputs) {

        // 1️⃣ sempre ordinamento per retry
        List<PaperTrackerDryRunOutputs> sortedByRetry = actualOutputs.stream()
                .sorted(Comparator.comparingInt(o -> extractPcRetry(o.getTrackingId())))
                .toList();

        if (ocrStatusEnum != OcrStatusEnum.RUN) {
            return sortedByRetry;
        }

        // 2️⃣ in RUN ordino per created
        List<PaperTrackerDryRunOutputs> sorted = sortedByRetry.stream()
                .sorted(Comparator.comparing(PaperTrackerDryRunOutputs::getCreated))
                .toList();

        // 3️⃣ PRIORITY REORDER (gestisce duplicati in modo stabile)
        var reorder = reorderRules.get(scenario.getName());
        if (reorder != null) {

            var result = new ArrayList<>(sorted);
            int index = findIndexByStatus(result, reorder.statusCode);

            if (index != -1 && result.size() >= 2) {
                PaperTrackerDryRunOutputs element = result.remove(index);
                result.add(reorder.position, element);
            }

            return result;
        }

        // 4️⃣ SWAP RULES (logica legacy)
        var rules = swapRules.get(scenario.getName());
        if (rules == null) {
            return sorted;
        }

        var result = new ArrayList<>(sorted);
        rules.forEach(rule -> swapIfPresent(result, rule));
        return result;
    }

    private static int extractPcRetry(String requestId) {
        int index = requestId.lastIndexOf("PCRETRY_");
        if (index == -1) {
            return Integer.MAX_VALUE;
        }
        return Integer.parseInt(requestId.substring(index + 8));
    }

    private static void swapIfPresent(List<PaperTrackerDryRunOutputs> list, SwapRule rule) {

        int index1 = findIndexByStatus(list, rule.first());
        int index2 = findIndexByStatus(list, rule.second());

        if (index1 != -1 && index2 != -1) {
            Collections.swap(list, index1, index2);
        }
    }

    private static int findIndexByStatus(List<PaperTrackerDryRunOutputs> list,
                                         String status) {

        for (int i = 0; i < list.size(); i++) {
            if (status.equals(list.get(i).getStatusDetail())) {
                return i;
            }
        }
        return -1;
    }


    private static void verifyOutputAttachments(List<Attachment> expected,
                                                List<Attachment> actual,
                                                int parentIndex) {

        if (CollectionUtils.isEmpty(expected)) {
            assertTrue(actual == null || actual.isEmpty(),
                    "Attachments should be empty at output index " + parentIndex);
            return;
        }
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size(), "Mismatch attachments count at output index " + parentIndex);
        for (int i = 0; i < expected.size(); i++) {
            Attachment expAtt = expected.get(i);
            Attachment actAtt = actual.get(i);
            assertAll("Attachment at output " + parentIndex + ", position " + i,
                    () -> assertEquals(expAtt.getId(), actAtt.getId()),
                    () -> assertEquals(expAtt.getDocumentType(), actAtt.getDocumentType()),
                    () -> assertEquals(expAtt.getUri(), actAtt.getUri()),
                    () -> assertEquals(expAtt.getDate(), actAtt.getDate())
            );
        }
    }

    private record SwapRule(String first, String second) {}
    private record ReorderRule(String statusCode, Integer position) {}
}
