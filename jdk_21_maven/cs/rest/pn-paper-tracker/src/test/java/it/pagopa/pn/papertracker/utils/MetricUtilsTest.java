package it.pagopa.pn.papertracker.utils;

import it.pagopa.pn.commons.log.dto.metrics.Dimension;
import it.pagopa.pn.commons.log.dto.metrics.GeneralMetric;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetricUtilsTest {

    @Test
    void generateDimension_createsCorrectDimension() {
        Dimension dimension = MetricUtils.generateDimension(MetricUtils.DimensionName.ERROR_CATEGORY, "TestValue");
        assertEquals("errorCategory", dimension.getName());
        assertEquals("TestValue", dimension.getValue());
    }

    @Test
    void generateGeneralMetric_createsMetricWithoutUnit() {
        GeneralMetric metric = MetricUtils.generateGeneralMetric(
                MetricUtils.MetricName.ERROR_COUNT,
                5,
                List.of(new Dimension("key", "value"))
        );

        assertEquals("paper-tracker-errors", metric.getNamespace());
        assertEquals("ERROR_COUNT", metric.getMetrics().getFirst().getName());
        assertEquals(5, metric.getMetrics().getFirst().getValue());
        assertEquals("key", metric.getDimensions().getFirst().getName());
        assertEquals("value", metric.getDimensions().getFirst().getValue());
        assertTrue(metric.getTimestamp() > 0);
        assertNull(metric.getUnit());
    }

    @Test
    void generateGeneralMetric_createsMetricWithUnit() {
        GeneralMetric metric = MetricUtils.generateGeneralMetric(
                MetricUtils.MetricName.ERROR_COUNT,
                10,
                List.of(new Dimension("key", "value")),
                MetricUtils.MetricUnit.COUNT
        );

        assertEquals("paper-tracker-errors", metric.getNamespace());
        assertEquals("ERROR_COUNT", metric.getMetrics().getFirst().getName());
        assertEquals(10, metric.getMetrics().getFirst().getValue());
        assertEquals("key", metric.getDimensions().getFirst().getName());
        assertEquals("value", metric.getDimensions().getFirst().getValue());
        assertTrue(metric.getTimestamp() > 0);
        assertEquals("Count", metric.getUnit());
    }
}
