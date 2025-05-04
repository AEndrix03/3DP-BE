package it.aredegalli.printer.model.communication;

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
@Table(name = "notification")
public class Notification {
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_ref")
    private String userRef;

    private String title;
    private String body;
    private String severity;
    private Instant createdAt;

    @Column(name = "read")
    private Boolean readFlag;
}
