package it.aredegalli.printer.model.log;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "log_entry")
public class LogEntry {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(updatable = false, nullable = false)
    private UUID id;

    private Instant timestamp;

    @ManyToOne
    @JoinColumn(name = "level")
    private LogLevel level;

    private String logger;
    private String message;

    @Column(columnDefinition = "jsonb")
    private String context;
}
