package it.aredegalli.printer.model.slicing;

import it.aredegalli.printer.model.resource.FileResource;
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
@Table(name = "slicing_result")
public class SlicingResult {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_resource_id", nullable = false)
    private FileResource sourceFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_resource_id", nullable = false)
    private FileResource generatedFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slicing_property_id", nullable = false)
    private SlicingProperty slicingProperty;

    @Column(nullable = false)
    private long lines;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private Instant createdAt;

}
