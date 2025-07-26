package it.aredegalli.printer.model.slicing.queue;

import it.aredegalli.printer.model.model.Model;
import it.aredegalli.printer.model.slicing.property.SlicingProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "slicing_queue")
public class SlicingQueue {
    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "model_id")
    private Model model;

    @ManyToOne
    @JoinColumn(name = "slicing_property_id")
    private SlicingProperty slicingProperty;

    @ColumnDefault("5")
    @Column(name = "priority")
    private Integer priority;

    @Size(max = 20)
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", length = Integer.MAX_VALUE)
    private String errorMessage;

    @Column(name = "progress_percentage")
    private Integer progressPercentage;

}