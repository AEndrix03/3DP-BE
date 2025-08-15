package it.aredegalli.printer.service.job;

import it.aredegalli.printer.dto.job.JobDto;
import it.aredegalli.printer.dto.job.request.JobStartRequestDto;

import java.util.List;
import java.util.UUID;

public interface JobService {
    JobDto getJob(UUID id);

    List<JobDto> getAllJobs();

    UUID startJob(JobStartRequestDto startRequestDto);
}
