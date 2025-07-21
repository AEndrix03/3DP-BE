package it.aredegalli.printer.repository.slicing;

import it.aredegalli.printer.model.slicing.SlicingQueueResult;
import it.aredegalli.printer.repository.UUIDRepository;

import java.util.UUID;

public interface SlicingQueueResultRepository extends UUIDRepository<SlicingQueueResult> {

    void deleteBySlicingResultId(UUID id);
}