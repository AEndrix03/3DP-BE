package it.aredegalli.printer.model.audit;

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
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    /**
     * Timestamp dell’evento; inizializzato a ora corrente
     */
    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    /**
     * Id dell’assegnatario che ha scatenato l’evento (senza FK)
     */
    @Column(name = "assegnatario_id", nullable = false, length = 100)
    private String assegnatarioId;

    /**
     * Tipo di evento (FK a audit_event_type)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_type", nullable = false)
    private AuditEventType eventType;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Dati addizionali in JSONB
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
