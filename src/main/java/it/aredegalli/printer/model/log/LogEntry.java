package it.aredegalli.printer.model.log;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "log_entry", indexes = {
        @Index(name = "idx_log_entry_timestamp", columnList = "timestamp"),
        @Index(name = "idx_log_entry_logger", columnList = "logger"),
        @Index(name = "idx_log_entry_level", columnList = "level")
})
public class LogEntry {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level", nullable = false)
    private LogLevel level;

    @Column(name = "logger", nullable = false, length = 100)
    private String logger;

    // Fixed: Use TEXT instead of VARCHAR to avoid length limitations
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context", columnDefinition = "jsonb")
    private Map<String, Object> context;

    // Auto-set timestamp on creation
    @PrePersist
    public void prePersist() {
        if (this.timestamp == null) {
            this.timestamp = Instant.now();
        }
    }
}