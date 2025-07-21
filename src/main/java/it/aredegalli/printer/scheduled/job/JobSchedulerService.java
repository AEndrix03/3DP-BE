package it.aredegalli.printer.scheduled.job;

import it.aredegalli.printer.enums.job.JobStatusEnum;
import it.aredegalli.printer.model.job.Job;
import it.aredegalli.printer.repository.job.JobRepository;
import it.aredegalli.printer.service.log.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Service for scheduling and managing 3D printer jobs
 * Handles job queue processing and status management
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class JobSchedulerService {

    private final JobRepository jobRepository;
    private final LogService logService;

    /**
     * Check for jobs that need to be processed every minute
     */
    @Scheduled(fixedDelay = 60000) // 1 minute
    public void scheduleJobExecution() {
        try {
            logService.debug("JobSchedulerService", "Checking for jobs to schedule...");

            // Find jobs that are queued and ready to run
            List<Job> queuedJobs = jobRepository.findByStatusOrderByCreatedAtAsc(JobStatusEnum.QUEUED);

            if (!queuedJobs.isEmpty()) {
                logService.info("JobSchedulerService",
                        String.format("Found %d queued jobs to process", queuedJobs.size()));

                processQueuedJobs(queuedJobs);
            }

            // Check for stale running jobs (optional cleanup)
            cleanupStaleJobs();

        } catch (Exception e) {
            logService.error("JobSchedulerService",
                    "Error during job scheduling: " + e.getMessage());
        }
    }

    /**
     * Process queued jobs based on printer availability
     */
    private void processQueuedJobs(List<Job> queuedJobs) {
        for (Job job : queuedJobs) {
            try {
                // Check if printer is available for this job
                if (isPrinterAvailable(job)) {
                    startJob(job);
                }

            } catch (Exception e) {
                logService.error("JobSchedulerService",
                        String.format("Failed to start job %s: %s", job.getId(), e.getMessage()));
            }
        }
    }

    /**
     * Check if printer is available for the job
     */
    private boolean isPrinterAvailable(Job job) {
        if (job.getPrinter() == null) {
            logService.warn("JobSchedulerService",
                    String.format("Job %s has no assigned printer", job.getId()));
            return false;
        }

        // Check if printer has any running jobs
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

    /**
     * Start a job by updating its status
     */
    private void startJob(Job job) {
        logService.info("JobSchedulerService",
                String.format("Starting job %s on printer %s",
                        job.getId(), job.getPrinter().getId()));

        job.setStatus(JobStatusEnum.RUNNING);
        job.setStartedAt(Instant.now());

        jobRepository.save(job);

        logService.info("JobSchedulerService",
                String.format("Job %s started successfully", job.getId()));
    }

    /**
     * Cleanup jobs that have been running for too long
     */
    private void cleanupStaleJobs() {
        try {
            // Find jobs running for more than 24 hours (configurable)
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

                    // Could mark as failed or send notification
                    // For now, just log the issue
                }
            }

        } catch (Exception e) {
            logService.error("JobSchedulerService",
                    "Error during stale job cleanup: " + e.getMessage());
        }
    }

    /**
     * Manual method to restart job scheduling (for admin use)
     */
    public void forceJobProcessing() {
        logService.info("JobSchedulerService", "Manual job processing triggered");
        scheduleJobExecution();
    }

    /**
     * Get scheduler statistics
     */
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

    /**
     * Statistics container for scheduler metrics
     */
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

        // Getters
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