package it.pagopa.pn.papertracker.utils;

import it.pagopa.pn.commons.log.dto.metrics.Dimension;
import it.pagopa.pn.commons.log.dto.metrics.GeneralMetric;
import it.pagopa.pn.commons.log.dto.metrics.Metric;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

public class MetricUtils {
    private static final String METRIC_NAMESPACE = "paper-tracker-errors";

    private MetricUtils() {}

    public static Dimension generateDimension(DimensionName name, String value) {
        return new Dimension(name.getValue(), value);
    }

    public static GeneralMetric generateGeneralMetric(MetricName metricName, int metricValue, List<Dimension> dimensions) {
        return generateGeneralMetric(metricName, metricValue, dimensions, null);
    }

    public static GeneralMetric generateGeneralMetric(MetricName metricName, int metricValue, List<Dimension> dimensions, MetricUnit unit) {
        GeneralMetric generalMetric = new GeneralMetric();
        generalMetric.setNamespace(METRIC_NAMESPACE);
        generalMetric.setMetrics(List.of(new Metric(metricName.getValue(), metricValue)));
        generalMetric.setDimensions(dimensions);
        generalMetric.setTimestamp(Instant.now().toEpochMilli());
        if (unit != null) generalMetric.setUnit(unit.getValue());
        return generalMetric;
    }

    @Getter
    public enum MetricName {
        ERROR_COUNT("ERROR_COUNT");
        private final String value;
        MetricName(String value) { this.value = value; }
    }

    @Getter
    public enum MetricUnit {
        COUNT("Count");
        private final String value;
        MetricUnit(String value) { this.value = value; }
    }

    @Getter
    public enum DimensionName {
        ERROR_CATEGORY("errorCategory"),
        PRODUCT_TYPE("productType");
        private final String value;
        DimensionName(String value) { this.value = value; }
    }
}
