package it.aredegalli.printer.model.slicing;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "slicing_job_assignment")
public class SlicingJobAssignment {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "slicing_queue_id")
    private UUID slicingQueueId; // Reference to SlicingQueue

    @Column(name = "container_id", nullable = false)
    private UUID containerId; // Reference to SlicingContainer

    @Column(name = "assigned_at")
    @Builder.Default
    private Instant assignedAt = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_status", length = 50)
    @Builder.Default
    private SlicingJobAssignmentStatus assignmentStatus = SlicingJobAssignmentStatus.ASSIGNED;

    @Column(name = "assignment_priority")
    @Builder.Default
    private Integer assignmentPriority = 5;

    @Column(name = "queue_wait_time_seconds")
    private Integer queueWaitTimeSeconds;

    @Column(name = "execution_time_seconds")
    private Integer executionTimeSeconds;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "last_error_message", columnDefinition = "text")
    private String lastErrorMessage;

    @Column(name = "result_size_bytes")
    private Long resultSizeBytes;

    @Column(name = "result_lines")
    private Integer resultLines;
}