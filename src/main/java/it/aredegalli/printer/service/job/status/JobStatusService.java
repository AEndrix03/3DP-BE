package it.aredegalli.printer.service.job.status;

import it.aredegalli.printer.dto.job.JobStatusDto;
import it.aredegalli.printer.enums.job.JobStatusEnum;
import it.aredegalli.printer.model.job.JobStatus;

public interface JobStatusService {
    JobStatusDto getJobStatusByCode(String code);

    JobStatusDto getJobStatus(JobStatusEnum jobStatusEnum);
    
    JobStatus status(JobStatusEnum jobStatusEnum);
}
