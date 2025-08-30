package it.aredegalli.printer.scheduled.job;

import it.aredegalli.common.exception.NotFoundException;
import it.aredegalli.printer.dto.kafka.control.status.PrinterStartRequestDto;
import it.aredegalli.printer.enums.job.JobStatusEnum;
import it.aredegalli.printer.model.driver.Driver;
import it.aredegalli.printer.model.job.Job;
import it.aredegalli.printer.repository.driver.DriverRepository;
import it.aredegalli.printer.repository.job.JobRepository;
import it.aredegalli.printer.service.kafka.control.status.PrinterStatusControlService;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.resource.FileResourceService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class JobSchedulerService {

    @Value("${deployment.url}")
    private String deploymentUrl;

    private final JobRepository jobRepository;
    private final DriverRepository driverRepository;
    private final FileResourceService fileResourceService;
    private final PrinterStatusControlService printerStatusControlService;
    private final LogService logService;

    // Track jobs already being processed to avoid duplicates
    private final Set<String> processingJobs = ConcurrentHashMap.newKeySet();

    @Scheduled(fixedDelay = 1000) // 60 seconds - FIX: era 1 secondo!
    public void scheduleJobExecution() {
        try {
            List<Job> queuedJobs = jobRepository.findByStatusOrderByCreatedAtAsc(JobStatusEnum.QUEUED);
            List<Job> createdJobs = jobRepository.findByStatusOrderByCreatedAtAsc(JobStatusEnum.CREATED);

            if (!queuedJobs.isEmpty()) {
                logService.info("JobSchedulerService",
                        String.format("Found %d queued jobs to process", queuedJobs.size()));
                processQueuedJobs(queuedJobs);
            }

            if (!createdJobs.isEmpty()) {
                logService.info("JobSchedulerService",
                        String.format("Found %d created jobs to enqueue", createdJobs.size()));
                processCreatedJobs(createdJobs);
            }

            cleanupStaleJobs();
        } catch (Exception e) {
            logService.error("JobSchedulerService",
                    "Error during job scheduling: " + e.getMessage());
        }
    }

    private void processCreatedJobs(List<Job> createdJobs) {
        for (Job job : createdJobs) {
            job.setStatus(JobStatusEnum.QUEUED);
        }
        jobRepository.saveAll(createdJobs);
    }

    private void processQueuedJobs(List<Job> queuedJobs) {
        for (Job job : queuedJobs) {
            String jobKey = job.getId().toString();

            // Skip if already processing
            if (processingJobs.contains(jobKey)) {
                logService.debug("JobSchedulerService",
                        String.format("Job %s already being processed, skipping", jobKey));
                continue;
            }

            try {
                if (isPrinterAvailable(job)) {
                    processingJobs.add(jobKey);
                    startJob(job);
                }
            } catch (Exception e) {
                logService.error("JobSchedulerService",
                        String.format("Failed to start job %s: %s", job.getId(), e.getMessage()));
                processingJobs.remove(jobKey);
            }
        }
    }

    private boolean isPrinterAvailable(Job job) {
        if (job.getPrinter() == null) {
            logService.warn("JobSchedulerService",
                    String.format("Job %s has no assigned printer", job.getId()));
            return false;
        }

        List<Job> runningJobs = jobRepository.findByPrinterAndStatus(
                job.getPrinter(), JobStatusEnum.RUNNING);

        boolean available = runningJobs.isEmpty();

        if (!available) {
            logService.debug("JobSchedulerService",
                    String.format("Printer %s is busy with %d running jobs",
                            job.getPrinter().getId(), runningJobs.size()));
        }

        return available;
    }

    @Transactional
    protected void startJob(Job job) {
        try {
            logService.info("JobSchedulerService",
                    String.format("Starting job %s on printer %s",
                            job.getId(), job.getPrinter().getId()));

            job.setStatus(JobStatusEnum.RUNNING);
            job.setStartedAt(Instant.now());

            Driver driver = driverRepository.findById(job.getPrinter().getDriverId())
                    .orElseThrow(() -> new NotFoundException("Driver not found"));

            jobRepository.save(job);

            String gcodeJwtToken = this.fileResourceService.ensureResource(
                    job.getSlicingResult().getGeneratedFile().getId(), driver.getId());

            PrinterStartRequestDto startRequest = PrinterStartRequestDto.builder()
                    .driverId(driver.getId().toString())
                    .startGCode(driver.getCustomStartGCode())
                    .endGCode(driver.getCustomEndGCode())
                    .gcodeUrl(this.deploymentUrl + "/public/download?token=" + gcodeJwtToken)
                    .build();

            this.printerStatusControlService.startPrint(startRequest);

            logService.info("JobSchedulerService",
                    String.format("Job %s started successfully", job.getId()));
        } finally {
            // Always remove from processing set
            processingJobs.remove(job.getId().toString());
        }
    }

    private void cleanupStaleJobs() {
        try {
            Instant cutoffTime = Instant.now().minusSeconds(24 * 60 * 60);

            List<Job> staleJobs = jobRepository.findByStatusAndStartedAtBefore(
                    JobStatusEnum.RUNNING, cutoffTime);

            if (!staleJobs.isEmpty()) {
                logService.warn("JobSchedulerService",
                        String.format("Found %d stale running jobs", staleJobs.size()));

                for (Job staleJob : staleJobs) {
                    logService.warn("JobSchedulerService",
                            String.format("Job %s has been running since %s - may need intervention",
                                    staleJob.getId(), staleJob.getStartedAt()));
                    // Remove from processing set if stuck
                    processingJobs.remove(staleJob.getId().toString());
                }
            }

            // Clean up old entries from processingJobs set
            if (processingJobs.size() > 100) {
                processingJobs.clear();
                logService.info("JobSchedulerService", "Cleared processing jobs tracking set");
            }

        } catch (Exception e) {
            logService.error("JobSchedulerService",
                    "Error during stale job cleanup: " + e.getMessage());
        }
    }

    public void forceJobProcessing() {
        logService.info("JobSchedulerService", "Manual job processing triggered");
        scheduleJobExecution();
    }

    public SchedulerStats getSchedulerStats() {
        try {
            long totalJobs = jobRepository.count();
            long queuedJobs = jobRepository.countByStatus(JobStatusEnum.QUEUED);
            long runningJobs = jobRepository.countByStatus(JobStatusEnum.RUNNING);
            long completedJobs = jobRepository.countByStatus(JobStatusEnum.COMPLETED);
            long failedJobs = jobRepository.countByStatus(JobStatusEnum.FAILED);

            return new SchedulerStats(totalJobs, queuedJobs, runningJobs,
                    completedJobs, failedJobs);

        } catch (Exception e) {
            logService.error("JobSchedulerService",
                    "Error getting scheduler stats: " + e.getMessage());
            return new SchedulerStats(0, 0, 0, 0, 0);
        }
    }

    public static class SchedulerStats {
        private final long totalJobs;
        private final long queuedJobs;
        private final long runningJobs;
        private final long completedJobs;
        private final long failedJobs;

        public SchedulerStats(long totalJobs, long queuedJobs, long runningJobs,
                              long completedJobs, long failedJobs) {
            this.totalJobs = totalJobs;
            this.queuedJobs = queuedJobs;
            this.runningJobs = runningJobs;
            this.completedJobs = completedJobs;
            this.failedJobs = failedJobs;
        }

        public long getTotalJobs() {
            return totalJobs;
        }

        public long getQueuedJobs() {
            return queuedJobs;
        }

        public long getRunningJobs() {
            return runningJobs;
        }

        public long getCompletedJobs() {
            return completedJobs;
        }

        public long getFailedJobs() {
            return failedJobs;
        }

        public double getSuccessRate() {
            return totalJobs > 0 ? (double) completedJobs / totalJobs * 100 : 0;
        }
    }
}