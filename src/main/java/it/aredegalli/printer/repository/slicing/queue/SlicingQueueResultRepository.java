package it.aredegalli.printer.repository.slicing.queue;

import it.aredegalli.printer.model.slicing.queue.SlicingQueueResult;
import it.aredegalli.printer.repository.UUIDRepository;

import java.util.UUID;

public interface SlicingQueueResultRepository extends UUIDRepository<SlicingQueueResult> {

    void deleteBySlicingResultId(UUID id);
}