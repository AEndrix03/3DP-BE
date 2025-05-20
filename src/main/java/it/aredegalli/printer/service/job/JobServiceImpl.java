package it.aredegalli.printer.service.job;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    /*private final JobRepository jobRepository;
    private final LogService log;
    private final JobStatusService jobStatusService;
    private final JobHistoryRepository jobHistoryRepository;
    private final JobMapper jobMapper;
    private final PrinterRepository printerRepository;
    private final FileResourceRepository fileResourceRepository;
    private final FileResourceService fileResourceService;

    @Override
    public JobDto getJob(UUID id) {
        return jobRepository.findById(id).stream()
                .map(jobMapper::toDto)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Job not found"));
    }

    @Override
    public List<JobDto> getAllJobs() {
        return jobRepository.findAll().stream()
                .map(jobMapper::toDto)
                .toList();
    }

    @Override
    public UUID startJob(UUID printerId, UUID resourceId) {
        return this.startJob(printerId, resourceId, null);
    }

    @Override
    public UUID startJob(UUID printerId, byte[] resourceId, JobStartRequestDto params) {
        Printer printer = printerRepository.findById(printerId)
                .orElseThrow(() -> new NotFoundException("Printer not found"));
        FileResource fileResource = fileResourceRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("FileResource not found"));

        Job.JobBuilder jobBuilder = Job.builder()
                .printer(printer)
                .fileResource(fileResource)
                .status(jobStatusService.status(JobStatusEnum.CREATED))
                .createdAt(Instant.now());

        if (params != null) {
            jobBuilder = jobBuilder.startOffsetLine(params.getStartOffsetLine());
        }

        log.info("JobServiceImpl", "Starting job for printer: " + printer.getId() + " with file: " + fileResource.getId());
        return jobRepository.save(jobBuilder.build()).getId();
    }

    @Override
    public UUID updateJob(@NotNull UUID jobId, @NotNull JobUpdateRequestDto params) {
        Job job = getJobById(jobId);

        if (!job.getStatus().getCode().equals(JobStatusEnum.RUNNING.getCode())) {
            throw new BadRequestException("Job is not running");
        }

        job.setProgress(params.getProgress());

        if (job.getProgress() >= job.getSlicingResult().getLines()) {
            return this.completeJob(job.getId(), "Reached end of file");
        }

        log.info("JobServiceImpl", "Updating job: " + job.getId());
        return jobRepository.save(job).getId();
    }

    @Override
    public UUID completeJob(@NotNull UUID jobId, String reason) {
        Job job = getJobById(jobId);

        if (!job.getStatus().getCode().equals(JobStatusEnum.RUNNING.getCode())) {
            throw new BadRequestException("Job is not running");
        }

        changeJobStatus(job, JobStatusEnum.COMPLETED, reason);

        log.info("JobServiceImpl", "Completing job: " + job.getId());
        return jobRepository.save(job).getId();
    }

    @Override
    public UUID cancelJob(@NotNull UUID jobId, String reason) {
        Job job = getJobById(jobId);

        if (!job.getStatus().getCode().equals(JobStatusEnum.CREATED.getCode())
                && !job.getStatus().getCode().equals(JobStatusEnum.QUEUED.getCode())
                && !job.getStatus().getCode().equals(JobStatusEnum.RUNNING.getCode())) {
            throw new BadRequestException("Job is not cancellable. Status: " + job.getStatus().getCode());
        }

        changeJobStatus(job, JobStatusEnum.CANCELLED, reason);

        log.info("JobServiceImpl", "Cancelling job: " + job.getId());
        return jobRepository.save(job).getId();
    }

    @Override
    public UUID enqueue(@NotNull UUID jobId) {
        Job job = getJobById(jobId);

        if (!job.getStatus().getCode().equals(JobStatusEnum.CREATED.getCode())) {
            throw new BadRequestException("Job is not enqueuable");
        }

        changeJobStatus(job, JobStatusEnum.QUEUED, "Enqueued");

        log.info("JobServiceImpl", "Enqueuing job: " + job.getId());
        return jobRepository.save(job).getId();
    }

    @Override
    public UUID pauseJob(@NotNull UUID jobId, String reason) {
        Job job = getJobById(jobId);

        if (!job.getStatus().getCode().equals(JobStatusEnum.RUNNING.getCode())) {
            throw new BadRequestException("Job is not running");
        }

        changeJobStatus(job, JobStatusEnum.PAUSED, reason);

        log.info("JobServiceImpl", "Pausing job: " + job.getId());
        return jobRepository.save(job).getId();
    }

    @Override
    public UUID resumeJob(@NotNull UUID jobId) {
        Job job = getJobById(jobId);

        if (!job.getStatus().getCode().equals(JobStatusEnum.PAUSED.getCode())) {
            throw new BadRequestException("Job is not paused");
        }

        changeJobStatus(job, JobStatusEnum.RUNNING, "Resumed");

        log.info("JobServiceImpl", "Resuming job: " + job.getId());
        return jobRepository.save(job).getId();
    }

    @Override
    public UUID runJob(UUID jobId) {
        Job job = getJobById(jobId);

        if (!job.getStatus().getCode().equals(JobStatusEnum.QUEUED.getCode())) {
            throw new BadRequestException("Job is not queued");
        }

        changeJobStatus(job, JobStatusEnum.RUNNING, "Running");

        log.info("JobServiceImpl", "Running job: " + job.getId());
        return jobRepository.save(job).getId();
    }

    @Override
    public List<JobDto> getAllJobsByPrinterId(UUID printerId) {
        return jobRepository.findAllByPrinterId(printerId).stream()
                .map(jobMapper::toDto)
                .toList();
    }

    private void changeJobStatus(@NotNull Job job, @NotNull JobStatusEnum newStatus, String reason) {
        JobStatus oldStatus = job.getStatus();
        JobStatus _newStatus = jobStatusService.status(newStatus);
        job.setStatus(_newStatus);

        jobHistoryRepository.save(JobHistory.builder()
                .job(job)
                .changedAt(Instant.now())
                .fromStatus(oldStatus)
                .toStatus(_newStatus)
                .reason(reason)
                .build());
    }

    private Job getJobById(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));
    }*/

}
