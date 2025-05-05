package it.aredegalli.printer.service.job;

import it.aredegalli.printer.dto.job.JobDto;
import it.aredegalli.printer.dto.job.request.JobStartRequestDto;
import it.aredegalli.printer.dto.job.request.JobUpdateRequestDto;
import it.aredegalli.printer.enums.audit.AuditEventTypeEnum;
import it.aredegalli.printer.service.audit.annotation.Audit;
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

    @Audit(event = AuditEventTypeEnum.JOB_CANCELLED, description = "Job cancelled")
    UUID cancelJob(@NotNull UUID jobId, String reason);

    @Audit(event = AuditEventTypeEnum.JOB_ENQUEUED, description = "Job enqueued")
    UUID enqueue(@NotNull UUID jobId);

    @Audit(event = AuditEventTypeEnum.JOB_PAUSED, description = "Job paused")
    UUID pauseJob(@NotNull UUID jobId, String reason);

    @Audit(event = AuditEventTypeEnum.JOB_PAUSED, description = "Job paused")
    UUID resumeJob(@NotNull UUID jobId);

    @Audit(event = AuditEventTypeEnum.JOB_RUN, description = "Job running")
    UUID runJob(UUID jobId);

    List<JobDto> getAllJobsByPrinterId(UUID printerId);
}
