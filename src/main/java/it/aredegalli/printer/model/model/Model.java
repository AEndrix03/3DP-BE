package it.aredegalli.printer.model.model;

import it.aredegalli.printer.model.resource.FileResource;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "model", indexes = {
        @Index(name = "idx_model_name", columnList = "name"),
        @Index(name = "idx_model_active", columnList = "active"),
        @Index(name = "idx_model_created_at", columnList = "created_at")
})
public class Model {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    // Fixed: Use Hibernate annotations for automatic timestamp handling
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "active")
    @Builder.Default
    private boolean active = true;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    @JoinColumn(name = "file_resource_id", referencedColumnName = "id", nullable = false)
    private FileResource fileResource;

    // Manual timestamp management if needed
    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}