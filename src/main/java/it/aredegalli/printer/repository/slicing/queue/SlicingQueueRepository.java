package it.aredegalli.printer.repository.slicing.queue;

import it.aredegalli.printer.model.slicing.queue.SlicingQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SlicingQueueRepository extends JpaRepository<SlicingQueue, UUID> {

    @Query("SELECT sq FROM SlicingQueue sq " +
            "WHERE sq.status = 'QUE' " +
            "ORDER BY sq.priority DESC, sq.createdAt ASC")
    List<SlicingQueue> findNextInQueue();

    @Query("SELECT sq FROM SlicingQueue sq " +
            "WHERE sq.status = :status " +
            "ORDER BY sq.priority DESC, sq.createdAt ASC")
    List<SlicingQueue> findByStatusOrderByPriorityDescCreatedAtAsc(@Param("status") String status);

    @Query("SELECT sq FROM SlicingQueue sq " +
            "WHERE sq.model.id = :modelId " +
            "ORDER BY sq.createdAt DESC")
    List<SlicingQueue> findByModelId(@Param("modelId") UUID modelId);

    @Query("SELECT sq FROM SlicingQueue sq " +
            "WHERE sq.createdByUserId = :userId " +
            "ORDER BY CASE sq.status " +
            "WHEN 'PRO' THEN 1 " +
            "WHEN 'QUE' THEN 2 " +
            "WHEN 'COM' THEN 3 " +
            "WHEN 'FAI' THEN 4 " +
            "END, sq.createdAt DESC")
    List<SlicingQueue> findByCreatedByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(sq) FROM SlicingQueue sq WHERE sq.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT sq FROM SlicingQueue sq " +
            "WHERE sq.status = :status " +
            "AND sq.startedAt IS NOT NULL " +
            "AND sq.startedAt < :threshold")
    List<SlicingQueue> findStaleProcessingJobs(
            @Param("status") String status,
            @Param("threshold") Instant threshold);
}