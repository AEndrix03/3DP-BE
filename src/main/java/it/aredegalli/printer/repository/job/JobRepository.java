package it.aredegalli.printer.repository.job;

import it.aredegalli.printer.model.job.Job;
import it.aredegalli.printer.repository.UUIDRepository;

import java.util.List;
import java.util.UUID;

public interface JobRepository extends UUIDRepository<Job> {

    List<Job> findAllByPrinterId(UUID printerId);

}
