package it.aredegalli.printer.scheduled.job;

import it.aredegalli.printer.enums.job.JobStatusEnum;
import it.aredegalli.printer.model.job.Job;
import it.aredegalli.printer.repository.job.JobRepository;
import it.aredegalli.printer.service.kafka.control.check.PrinterCheckService;
import it.aredegalli.printer.service.log.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class JobCheckSchedulerService {

    private final JobRepository jobRepository;
    private final PrinterCheckService printerCheckService;
    private final LogService logService;

    // Track last check time per job to avoid flooding
    private final Map<String, Instant> lastCheckTime = new ConcurrentHashMap<>();
    private static final long MIN_CHECK_INTERVAL_MS = 25000; // 25 seconds minimum between checks

    @Scheduled(fixedDelay = 30000) // 30 seconds
    public void scheduleJobCheckExecution() {
        try {
            List<Job> runningJobs = jobRepository.findByStatusInOrderByCreatedAtAsc(List.of(
                    JobStatusEnum.RUNNING,
                    JobStatusEnum.FAILED,
                    JobStatusEnum.PRECHECK,
                    JobStatusEnum.HOMING,
                    JobStatusEnum.LOADING,
                    JobStatusEnum.HEATING,
                    JobStatusEnum.UNKNOWN
            ));
            if (!runningJobs.isEmpty()) {
                Instant now = Instant.now();

                for (Job job : runningJobs) {
                    String jobKey = job.getId().toString();
                    Instant lastCheck = lastCheckTime.get(jobKey);

                    // Skip if checked too recently
                    if (lastCheck != null &&
                            now.toEpochMilli() - lastCheck.toEpochMilli() < MIN_CHECK_INTERVAL_MS) {
                        logService.debug("JobCheckSchedulerService",
                                String.format("Skipping check for job %s - checked %d ms ago",
                                        jobKey, now.toEpochMilli() - lastCheck.toEpochMilli()));
                        continue;
                    }

                    logService.info("JobCheckSchedulerService",
                            String.format("Sending check request for job %s to driver %s",
                                    job.getId(), job.getPrinter().getDriverId()));

                    this.printerCheckService.checkPrinter(
                            job.getPrinter().getDriverId(),
                            job.getId(),
                            "scheduled_30s"
                    );

                    lastCheckTime.put(jobKey, now);
                }

                // Clean up old entries
                cleanupOldCheckTimes(now);
            }
        } catch (Exception e) {
            logService.error("JobCheckSchedulerService",
                    "Error during job check scheduling: " + e.getMessage());
        }
    }

    private void cleanupOldCheckTimes(Instant now) {
        // Remove entries older than 5 minutes
        long cutoff = now.toEpochMilli() - 300000;
        lastCheckTime.entrySet().removeIf(entry ->
                entry.getValue().toEpochMilli() < cutoff
        );
    }
}