package it.aredegalli.printer.repository.slicing.queue;

import it.aredegalli.printer.enums.slicing.SlicingStatus;
import it.aredegalli.printer.model.slicing.queue.SlicingQueue;
import it.aredegalli.printer.repository.UUIDRepository;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public interface SlicingQueueRepository extends UUIDRepository<SlicingQueue> {

    List<SlicingQueue> findByStatusOrderByPriorityDescCreatedAtAsc(@Size(max = 20) String status);

    List<SlicingQueue> findByModelId(UUID modelId);

    default List<SlicingQueue> findNextInQueue() {
        return findByStatusOrderByPriorityDescCreatedAtAsc((SlicingStatus.QUEUED).getCode());
    }

    long countByStatus(@Size(max = 20) String status);
}