package it.aredegalli.printer.service.slicing.metrics;

import it.aredegalli.printer.model.slicing.metric.SlicingMetric;
import it.aredegalli.printer.model.slicing.result.SlicingResult;

import java.util.UUID;

public interface SlicingMetricsService {

    SlicingMetric calculateMetrics(SlicingResult result);

    SlicingMetric getMetricsBySlicingResultId(UUID slicingResultId);

    void saveMetrics(SlicingMetric metrics);
}