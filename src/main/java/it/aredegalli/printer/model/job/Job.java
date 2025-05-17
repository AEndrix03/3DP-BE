package it.aredegalli.printer.model.job;

import it.aredegalli.printer.model.printer.Printer;
import it.aredegalli.printer.model.slicing.SlicingResult;
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
@Table(name = "job")
public class Job {
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "printer_id")
    private Printer printer;

    @ManyToOne
    @JoinColumn(name = "file_hash", referencedColumnName = "id")
    private SlicingResult slicingResult;

    @ManyToOne
    @JoinColumn(name = "status")
    private JobStatus status;

    private Long progress;

    @Column(name = "start_offset_line")
    private Long startOffsetLine;

    private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;
}
