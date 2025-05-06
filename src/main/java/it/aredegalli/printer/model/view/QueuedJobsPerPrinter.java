package it.aredegalli.printer.model.view;

import it.aredegalli.printer.model.job.Job;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "queued_jobs_per_printer")
public class QueuedJobsPerPrinter {

    @Id
    @Column(name = "printer_id")
    private UUID printerId;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

}
