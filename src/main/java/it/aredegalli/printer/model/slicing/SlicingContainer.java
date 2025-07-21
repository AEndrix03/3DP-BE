package it.aredegalli.printer.model.slicing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "slicing_container")
public class SlicingContainer {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "container_id", unique = true, nullable = false, length = 100)
    private String containerId;

    @Column(name = "container_name", nullable = false, length = 100)
    private String containerName;

    @Column(name = "container_type", length = 50)
    @Builder.Default
    private String containerType = "standard";

    @Column(name = "host", nullable = false)
    private String host;

    @Column(name = "port", nullable = false)
    private Integer port;

    // Fixed enum mapping with custom converter
    @Column(name = "status", length = 50)
    @Convert(converter = ContainerStatus.ContainerStatusConverter.class)
    @Builder.Default
    private ContainerStatus status = ContainerStatus.UNKNOWN;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 5;

    @Column(name = "max_concurrent_jobs")
    @Builder.Default
    private Integer maxConcurrentJobs = 1;

    @Column(name = "current_active_jobs")
    @Builder.Default
    private Integer currentActiveJobs = 0;

    @Column(name = "total_jobs_processed")
    @Builder.Default
    private Long totalJobsProcessed = 0L;

    @Column(name = "total_jobs_failed")
    @Builder.Default
    private Long totalJobsFailed = 0L;

    // Resource information
    @Column(name = "memory_limit_mb")
    private Integer memoryLimitMb;

    @Column(name = "cpu_limit", precision = 5, scale = 2)  // Fixed precision for double
    private Double cpuLimit;

    @Column(name = "disk_space_mb")
    private Long diskSpaceMb;

    // Timestamps
    @Column(name = "last_health_check")
    private Instant lastHealthCheck;

    @Column(name = "last_job_started")
    private Instant lastJobStarted;

    @Column(name = "last_job_completed")
    private Instant lastJobCompleted;

    @Column(name = "uptime_seconds")
    @Builder.Default
    private Long uptimeSeconds = 0L;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "created_by", length = 100)
    @Builder.Default
    private String createdBy = "system";

    @Column(name = "engine_version", length = 100)
    private String engineVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", columnDefinition = "jsonb")
    private Map<String, Object> configuration;

    // Helper methods
    public boolean isAvailable() {
        return status == ContainerStatus.HEALTHY &&
                currentActiveJobs < maxConcurrentJobs;
    }

    public double getLoadPercentage() {
        return maxConcurrentJobs > 0 ?
                (double) currentActiveJobs / maxConcurrentJobs * 100 : 0;
    }

    // Update timestamp on entity changes
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}