package it.aredegalli.printer.repository.job;

import it.aredegalli.printer.model.job.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobStatusRepository extends JpaRepository<JobStatus, String> {
}
