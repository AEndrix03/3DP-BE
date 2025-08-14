package it.aredegalli.printer.model.job;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_progress_snapshot",
        indexes = {
                @Index(name = "idx_job_progress_job_time", columnList = "job_id,recordedAt DESC"),
                @Index(name = "idx_job_progress_recorded_at", columnList = "recordedAt DESC"),
                @Index(name = "idx_job_progress_active_jobs", columnList = "job_id,recordedAt DESC")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobProgressSnapshot {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_job_progress_job"))
    private Job job;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, columnDefinition = "timestamp with time zone")
    private Instant recordedAt;

    // Dati direttamente mappati dal PrinterCheckResponseDto
    @Column(name = "status_code", length = 50)
    private String statusCode;

    @Column(name = "x_position", precision = 8, scale = 3)
    private BigDecimal xPosition;

    @Column(name = "y_position", precision = 8, scale = 3)
    private BigDecimal yPosition;

    @Column(name = "z_position", precision = 8, scale = 3)
    private BigDecimal zPosition;

    @Column(name = "e_position", precision = 10, scale = 3)
    private BigDecimal ePosition;

    @Column(name = "feed", precision = 8, scale = 2)
    private BigDecimal feed;

    @Column(name = "current_layer")
    @Min(value = 0, message = "Current layer must be non-negative")
    private Integer currentLayer;

    @Column(name = "layer_height", precision = 6, scale = 3)
    private BigDecimal layerHeight;

    @Column(name = "extruder_status", length = 30)
    private String extruderStatus;

    @Column(name = "extruder_temp", precision = 5, scale = 1)
    private BigDecimal extruderTemp;

    @Column(name = "bed_temp", precision = 5, scale = 1)
    private BigDecimal bedTemp;

    @Column(name = "fan_status", length = 30)
    private String fanStatus;

    @Column(name = "fan_speed", precision = 5, scale = 2)
    @DecimalMin(value = "0.0", message = "Fan speed must be non-negative")
    @DecimalMax(value = "100.0", message = "Fan speed cannot exceed 100")
    private BigDecimal fanSpeed;

    @Column(name = "command_offset")
    private Long commandOffset;

    @Column(name = "last_command", columnDefinition = "text")
    private String lastCommand;

    @Column(name = "average_speed", precision = 7, scale = 2)
    private BigDecimal averageSpeed;

    @Column(name = "exceptions", columnDefinition = "text")
    private String exceptions;

    @Column(name = "logs", columnDefinition = "text")
    private String logs;

    // Informazioni aggiuntive calcolate
    @Column(name = "local_progress_percentage", precision = 5, scale = 2)
    @DecimalMin(value = "0.0", message = "Progress percentage must be non-negative")
    @DecimalMax(value = "100.0", message = "Progress percentage cannot exceed 100")
    private BigDecimal localProgressPercentage;

    @Column(name = "estimated_remaining_time_min")
    private Integer estimatedRemainingTimeMin;

    @Column(name = "material_used_g", precision = 8, scale = 3)
    private BigDecimal materialUsedG;

    @Column(name = "error_count", nullable = false, columnDefinition = "integer default 0")
    @Min(value = 0, message = "Error count must be non-negative")
    private Integer errorCount = 0;

    @Column(name = "warning_count", nullable = false, columnDefinition = "integer default 0")
    @Min(value = 0, message = "Warning count must be non-negative")
    private Integer warningCount = 0;

}