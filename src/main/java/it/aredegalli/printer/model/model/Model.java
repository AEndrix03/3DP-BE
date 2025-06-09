package it.aredegalli.printer.model.model;

import it.aredegalli.printer.model.slicing.FileResource;
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
@Table(name = "model")
public class Model {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private Instant updatedAt;

    @Column(name = "active")
    @Builder.Default
    private boolean active = true;

    @OneToOne
    @JoinColumn(referencedColumnName = "id", nullable = false)
    private FileResource fileResource;
}
