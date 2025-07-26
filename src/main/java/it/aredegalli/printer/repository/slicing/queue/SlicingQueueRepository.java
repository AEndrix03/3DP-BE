package it.aredegalli.printer.repository.slicing.queue;

import it.aredegalli.printer.enums.slicing.SlicingStatus;
import it.aredegalli.printer.model.slicing.queue.SlicingQueue;
import it.aredegalli.printer.repository.UUIDRepository;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SlicingQueueRepository extends UUIDRepository<SlicingQueue> {

    List<SlicingQueue> findByStatusOrderByPriorityDescCreatedAtAsc(@Size(max = 20) String status);

    List<SlicingQueue> findByModelId(UUID modelId);

    default List<SlicingQueue> findNextInQueue() {
        return findByStatusOrderByPriorityDescCreatedAtAsc((SlicingStatus.QUEUED).getCode());
    }

    long countByStatus(@Size(max = 20) String status);

    @Query("SELECT sq FROM SlicingQueue sq WHERE sq.createdByUserId = :createdByUserId " +
            "ORDER BY CASE sq.status " +
            "WHEN 'PROCESSING' THEN 1 " +
            "WHEN 'QUEUED' THEN 2 " +
            "WHEN 'COMPLETED' THEN 3 " +
            "WHEN 'FAILED' THEN 4 " +
            "END, sq.createdAt DESC")
    List<SlicingQueue> findByCreatedByUserId(@Param("createdByUserId") String createdByUserId);
}