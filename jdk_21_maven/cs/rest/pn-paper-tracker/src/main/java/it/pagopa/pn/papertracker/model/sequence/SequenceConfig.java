package it.pagopa.pn.papertracker.model.sequence;

import java.util.Map;
import java.util.Set;

public record SequenceConfig(
        Set<String> sequenceStatusCodes,
        Set<String> requiredStatusCodes,
        Set<String> dateValidationGroupForFinalEvents,
        Set<String> dateValidationGroupForStockEvents,
        Set<String> requiredAttachments,
        Map<String, Set<String>> validAttachments
) {}
