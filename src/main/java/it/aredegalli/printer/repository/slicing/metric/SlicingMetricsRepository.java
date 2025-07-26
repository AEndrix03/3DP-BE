package it.aredegalli.printer.repository.slicing.metric;

import it.aredegalli.printer.model.slicing.metric.SlicingMetric;
import it.aredegalli.printer.repository.UUIDRepository;

import java.util.Optional;
import java.util.UUID;

public interface SlicingMetricsRepository extends UUIDRepository<SlicingMetric> {

    Optional<SlicingMetric> findBySlicingResultId(UUID slicingResultId);

    void deleteBySlicingResultId(UUID slicingResultId);
}