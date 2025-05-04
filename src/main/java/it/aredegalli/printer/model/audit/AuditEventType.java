package it.aredegalli.printer.model.audit;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "audit_event_type")
public class AuditEventType {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "description", nullable = false, length = 50)
    private String description;
}
