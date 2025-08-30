package it.aredegalli.printer.repository.job;

import it.aredegalli.printer.enums.job.JobStatusEnum;
import it.aredegalli.printer.model.job.Job;
import it.aredegalli.printer.model.printer.Printer;
import it.aredegalli.printer.repository.UUIDRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JobRepository extends UUIDRepository<Job> {

    List<Job> findAllByPrinterId(UUID printerId);

    List<Job> findByStatusOrderByCreatedAtAsc(JobStatusEnum jobStatusEnum);

    List<Job> findByStatusInOrderByCreatedAtAsc(List<JobStatusEnum> statuses);

    List<Job> findByPrinterAndStatus(Printer printer, JobStatusEnum status);

    List<Job> findByStatusAndStartedAtBefore(JobStatusEnum status, Instant startedAtBefore);

    long countByStatus(JobStatusEnum status);
}
