package it.aredegalli.printer.model.slicing;

import it.aredegalli.printer.repository.UUIDRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlicingContainerRepository extends UUIDRepository<SlicingContainer> {

    List<SlicingContainer> findByStatus(ContainerStatus status);

    List<SlicingContainer> findByContainerType(String containerType);

    List<SlicingContainer> findByStatusAndCurrentActiveJobsLessThanMaxConcurrentJobs(ContainerStatus status);

    Optional<SlicingContainer> findByContainerId(String containerId);

    @Query("SELECT c FROM SlicingContainer c WHERE c.status = 'HEALTHY' AND c.currentActiveJobs < c.maxConcurrentJobs ORDER BY c.priority ASC, c.currentActiveJobs ASC")
    List<SlicingContainer> findAvailableContainersOrderedByPriority();

    @Query("SELECT c FROM SlicingContainer c WHERE c.containerType = :containerType AND c.status = 'HEALTHY' AND c.currentActiveJobs < c.maxConcurrentJobs ORDER BY c.priority ASC")
    List<SlicingContainer> findAvailableContainersByType(@Param("containerType") String containerType);

    @Modifying
    @Query("UPDATE SlicingContainer c SET c.status = :status, c.lastHealthCheck = :timestamp, c.updatedAt = :timestamp WHERE c.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") ContainerStatus status, @Param("timestamp") Instant timestamp);

    @Modifying
    @Query("UPDATE SlicingContainer c SET c.currentActiveJobs = GREATEST(0, c.currentActiveJobs + :delta), c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :id")
    void updateActiveJobCount(@Param("id") UUID id, @Param("delta") int delta);

    @Query("SELECT COUNT(c) FROM SlicingContainer c WHERE c.status = 'HEALTHY'")
    long countHealthyContainers();

    @Query("SELECT c.containerType, COUNT(c) FROM SlicingContainer c WHERE c.status = 'HEALTHY' GROUP BY c.containerType")
    List<Object[]> countHealthyContainersByType();
}