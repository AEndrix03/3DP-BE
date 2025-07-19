package it.aredegalli.printer.service.slicing.metrics;

import it.aredegalli.printer.model.slicing.SlicingMetric;
import it.aredegalli.printer.model.slicing.SlicingResult;

import java.util.UUID;

public interface SlicingMetricsService {

    SlicingMetric calculateMetrics(SlicingResult result);

    SlicingMetric getMetricsBySlicingResultId(UUID slicingResultId);

    void saveMetrics(SlicingMetric metrics);
}