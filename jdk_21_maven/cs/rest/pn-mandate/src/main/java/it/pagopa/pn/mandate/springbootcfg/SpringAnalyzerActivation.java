package it.pagopa.pn.mandate.springbootcfg;

import io.micrometer.core.instrument.MeterRegistry;
import it.pagopa.pn.commons.utils.metrics.SpringAnalyzer;
import it.pagopa.pn.commons.utils.metrics.cloudwatch.CloudWatchMetricHandler;
import lombok.CustomLog;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

@Component
@CustomLog
@Import(CloudWatchMetricHandler.class)
public class SpringAnalyzerActivation extends SpringAnalyzer {

    MeterRegistry meterRegistry;
    CloudWatchMetricHandler cloudWatchMetricHandler;

    public SpringAnalyzerActivation(CloudWatchMetricHandler cloudWatchMetricHandler, MetricsEndpoint metricsEndpoint, MeterRegistry meterRegistry) {
        super(cloudWatchMetricHandler, metricsEndpoint);
        this.cloudWatchMetricHandler = cloudWatchMetricHandler;
        this.meterRegistry = meterRegistry;
    }
}
