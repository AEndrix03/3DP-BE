package it.aredegalli.printer.service.job;

import it.aredegalli.printer.dto.job.JobDto;
import it.aredegalli.printer.dto.job.request.JobStartRequestDto;
import it.aredegalli.printer.dto.job.request.JobUpdateRequestDto;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public interface JobService {
    JobDto getJob(UUID id);

    List<JobDto> getAllJobs();

    UUID startJob(UUID printerId, UUID resourceId);

    UUID startJob(UUID printerId, UUID resourceId, JobStartRequestDto params);

    UUID updateJob(UUID jobId, JobUpdateRequestDto params);

    UUID completeJob(@NotNull UUID jobId, String reason);

    UUID cancelJob(@NotNull UUID jobId, String reason);

    UUID enqueue(@NotNull UUID jobId);

    UUID pauseJob(@NotNull UUID jobId, String reason);

    UUID resumeJob(@NotNull UUID jobId);

    UUID runJob(UUID jobId);

    List<JobDto> getAllJobsByPrinterId(UUID printerId);
}
