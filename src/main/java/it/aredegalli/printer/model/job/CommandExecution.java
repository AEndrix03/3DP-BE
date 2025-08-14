package it.aredegalli.printer.model.job;

import it.aredegalli.printer.enums.kafka.PrinterCommandExecutionStatusEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Entità per il tracking dell'esecuzione dei comandi sui driver delle stampanti.
 * Gestisce la coda, l'esecuzione e il risultato dei comandi inviati ai driver.
 */
@Entity
@Table(name = "command_execution",
        indexes = {
                @Index(name = "idx_command_execution_driver", columnList = "driver_id,createdAt DESC"),
                @Index(name = "idx_command_execution_status", columnList = "status,createdAt DESC"),
                @Index(name = "idx_command_execution_priority", columnList = "priority ASC,createdAt ASC"),
                @Index(name = "idx_command_execution_driver_status_priority",
                        columnList = "driver_id,status,priority ASC")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandExecution {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "command", length = 500, nullable = false)
    @NotBlank(message = "Command cannot be empty")
    @Size(max = 500, message = "Command length cannot exceed 500 characters")
    private String command;

    @Column(name = "priority", nullable = false, columnDefinition = "integer default 5")
    @Min(value = 0, message = "Priority must be between 0 and 10")
    @Max(value = 10, message = "Priority must be between 0 and 10")
    @Builder.Default
    private Integer priority = 5;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    @NotNull(message = "Status cannot be null")
    private PrinterCommandExecutionStatusEnum status;

    @Column(name = "ok", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean ok = false;

    @Column(name = "exception", length = 1000)
    @Size(max = 1000, message = "Exception message cannot exceed 1000 characters")
    private String exception;

    @Column(name = "info", length = 1000)
    @Size(max = 1000, message = "Info cannot exceed 1000 characters")
    private String info;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, columnDefinition = "timestamp with time zone")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp with time zone")
    private Instant updatedAt;

    @Column(name = "started_at", columnDefinition = "timestamp with time zone")
    private Instant startedAt;

    @Column(name = "finished_at", columnDefinition = "timestamp with time zone")
    private Instant finishedAt;

}