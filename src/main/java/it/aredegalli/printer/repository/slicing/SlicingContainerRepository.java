package it.aredegalli.printer.repository.slicing;

import it.aredegalli.printer.model.slicing.ContainerStatus;
import it.aredegalli.printer.model.slicing.SlicingContainer;
import it.aredegalli.printer.repository.UUIDRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for SlicingContainer entities
 * Provides methods to manage Docker slicing containers
 */
public interface SlicingContainerRepository extends UUIDRepository<SlicingContainer> {

    /**
     * Find containers by status
     */
    List<SlicingContainer> findByStatus(ContainerStatus status);

    /**
     * Find containers by container type (e.g., "priority", "standard", "batch")
     */
    List<SlicingContainer> findByContainerType(String containerType);

    /**
     * Find containers that are healthy and have available job slots
     */
    @Query("SELECT c FROM SlicingContainer c WHERE c.status = 'HEALTHY' AND c.currentActiveJobs < c.maxConcurrentJobs")
    List<SlicingContainer> findAvailableContainers();

    /**
     * Find container by its Docker container ID
     */
    Optional<SlicingContainer> findByContainerId(String containerId);

    /**
     * Find available containers ordered by priority (best first)
     */
    @Query("SELECT c FROM SlicingContainer c WHERE c.status = 'HEALTHY' AND c.currentActiveJobs < c.maxConcurrentJobs ORDER BY c.priority ASC, c.currentActiveJobs ASC")
    List<SlicingContainer> findAvailableContainersOrderedByPriority();

    /**
     * Find available containers by type
     */
    @Query("SELECT c FROM SlicingContainer c WHERE c.containerType = :containerType AND c.status = 'HEALTHY' AND c.currentActiveJobs < c.maxConcurrentJobs ORDER BY c.priority ASC")
    List<SlicingContainer> findAvailableContainersByType(@Param("containerType") String containerType);

    /**
     * Update container status and last health check time
     */
    @Modifying
    @Query("UPDATE SlicingContainer c SET c.status = :status, c.lastHealthCheck = :timestamp, c.updatedAt = :timestamp WHERE c.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") ContainerStatus status, @Param("timestamp") Instant timestamp);

    /**
     * Update active job count (increment/decrement)
     */
    @Modifying
    @Query("UPDATE SlicingContainer c SET c.currentActiveJobs = GREATEST(0, c.currentActiveJobs + :delta), c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :id")
    void updateActiveJobCount(@Param("id") UUID id, @Param("delta") int delta);

    /**
     * Get count of healthy containers
     */
    @Query("SELECT COUNT(c) FROM SlicingContainer c WHERE c.status = 'HEALTHY'")
    long countHealthyContainers();

    /**
     * Get count of healthy containers by type
     */
    @Query("SELECT c.containerType, COUNT(c) FROM SlicingContainer c WHERE c.status = 'HEALTHY' GROUP BY c.containerType")
    List<Object[]> countHealthyContainersByType();

    /**
     * Find containers that need health check (last check older than interval)
     */
    @Query("SELECT c FROM SlicingContainer c WHERE c.lastHealthCheck IS NULL OR c.lastHealthCheck < :cutoffTime")
    List<SlicingContainer> findContainersNeedingHealthCheck(@Param("cutoffTime") Instant cutoffTime);

    /**
     * Find containers by host and port
     */
    Optional<SlicingContainer> findByHostAndPort(String host, Integer port);

    /**
     * Find containers with high load (above threshold)
     */
    @Query("SELECT c FROM SlicingContainer c WHERE (c.currentActiveJobs * 100.0 / c.maxConcurrentJobs) > :loadPercentage")
    List<SlicingContainer> findContainersWithHighLoad(@Param("loadPercentage") double loadPercentage);

    /**
     * Update container performance stats
     */
    @Modifying
    @Query("UPDATE SlicingContainer c SET c.totalJobsProcessed = c.totalJobsProcessed + 1, c.lastJobCompleted = CURRENT_TIMESTAMP, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :id")
    void incrementJobsProcessed(@Param("id") UUID id);

    /**
     * Update container failure stats
     */
    @Modifying
    @Query("UPDATE SlicingContainer c SET c.totalJobsFailed = c.totalJobsFailed + 1, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :id")
    void incrementJobsFailed(@Param("id") UUID id);

    /**
     * Reset active job count to zero (for cleanup/recovery)
     */
    @Modifying
    @Query("UPDATE SlicingContainer c SET c.currentActiveJobs = 0, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :id")
    void resetActiveJobCount(@Param("id") UUID id);

    /**
     * Find containers that have been inactive for too long
     */
    @Query("SELECT c FROM SlicingContainer c WHERE c.currentActiveJobs > 0 AND c.lastJobStarted < :cutoffTime")
    List<SlicingContainer> findStuckContainers(@Param("cutoffTime") Instant cutoffTime);

    /**
     * Update container configuration
     */
    @Modifying
    @Query("UPDATE SlicingContainer c SET c.maxConcurrentJobs = :maxJobs, c.memoryLimitMb = :memoryLimit, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :id")
    void updateContainerResources(@Param("id") UUID id, @Param("maxJobs") Integer maxJobs, @Param("memoryLimit") Integer memoryLimit);
}