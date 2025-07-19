package it.aredegalli.printer.repository.slicing;

import it.aredegalli.printer.enums.slicing.SlicingStatus;
import it.aredegalli.printer.model.slicing.SlicingQueue;
import it.aredegalli.printer.repository.UUIDRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface SlicingQueueRepository extends UUIDRepository<SlicingQueue> {

    List<SlicingQueue> findByStatusOrderByPriorityDescCreatedAtAsc(SlicingStatus status);

    List<SlicingQueue> findByModelId(UUID modelId);

    @Query("SELECT sq FROM SlicingQueue sq WHERE sq.status = 'QUEUED' ORDER BY sq.priority DESC, sq.createdAt ASC")
    List<SlicingQueue> findNextInQueue();

    long countByStatus(SlicingStatus status);
}