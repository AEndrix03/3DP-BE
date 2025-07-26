package it.aredegalli.printer.model.job;

import it.aredegalli.printer.enums.job.JobStatusEnum;
import it.aredegalli.printer.enums.job.JobStatusEnumConverter;
import it.aredegalli.printer.model.printer.Printer;
import it.aredegalli.printer.model.slicing.result.SlicingResult;
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
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "printer_id")
    private Printer printer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slicing_id", nullable = false)
    private SlicingResult slicingResult;

    @Convert(converter = JobStatusEnumConverter.class)
    @Column(name = "status", nullable = false, length = 3)
    private JobStatusEnum status;

    @Column
    private Integer progress;

    @Column(name = "start_offset_line")
    private Integer startOffsetLine;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;
}
