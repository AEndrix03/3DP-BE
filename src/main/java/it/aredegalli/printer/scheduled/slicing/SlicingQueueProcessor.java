package it.aredegalli.printer.scheduled.slicing;

import it.aredegalli.printer.enums.slicing.SlicingStatus;
import it.aredegalli.printer.model.slicing.queue.SlicingQueue;
import it.aredegalli.printer.repository.slicing.queue.SlicingQueueRepository;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.slicing.SlicingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class SlicingQueueProcessor implements HealthIndicator {

    private final SlicingQueueRepository slicingQueueRepository;
    private final SlicingService slicingService;
    private final LogService logService;

    @Value("${slicing.queue.processing.enabled:true}")
    private boolean processingEnabled;

    @Value("${slicing.queue.processing.max-concurrent:2}")
    private int maxConcurrentJobs;

    @Value("${slicing.queue.stale-timeout-minutes:30}")
    private int staleTimeoutMinutes;

    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);
    private final Set<String> currentlyProcessing = ConcurrentHashMap.newKeySet();
    private Instant lastProcessingTime = Instant.now();

    @Scheduled(fixedDelay = 15000)
    public void processQueue() {
        if (!processingEnabled) {
            return;
        }

        try {
            long currentProcessingJobs = slicingQueueRepository.countByStatus(SlicingStatus.PROCESSING.getCode());

            if (currentProcessingJobs >= maxConcurrentJobs) {
                return;
            }

            List<SlicingQueue> nextJobs = slicingQueueRepository.findNextInQueue();
            if (nextJobs.isEmpty()) {
                return;
            }

            int availableSlots = (int) (maxConcurrentJobs - currentProcessingJobs);
            int jobsToProcess = Math.min(nextJobs.size(), availableSlots);

            for (int i = 0; i < jobsToProcess; i++) {
                SlicingQueue job = nextJobs.get(i);
                String jobKey = job.getId().toString();

                if (currentlyProcessing.contains(jobKey)) {
                    continue;
                }

                currentlyProcessing.add(jobKey);
                processJobAsync(job);
            }

            lastProcessingTime = Instant.now();

        } catch (Exception e) {
            logService.error("SlicingQueueProcessor", "Error in queue processing: " + e.getMessage());
            failedCount.incrementAndGet();
        }
    }

    @Scheduled(fixedDelay = 300000)
    public void cleanupStaleJobs() {
        if (!processingEnabled) {
            return;
        }

        try {
            Instant staleThreshold = Instant.now().minus(staleTimeoutMinutes, ChronoUnit.MINUTES);

            List<SlicingQueue> staleJobs = slicingQueueRepository
                    .findByStatusOrderByPriorityDescCreatedAtAsc(SlicingStatus.PROCESSING.getCode())
                    .stream()
                    .filter(job -> job.getStartedAt() != null && job.getStartedAt().isBefore(staleThreshold))
                    .toList();

            for (SlicingQueue staleJob : staleJobs) {
                staleJob.setStatus(SlicingStatus.FAILED.getCode());
                staleJob.setErrorMessage("Job timed out after " + staleTimeoutMinutes + " minutes");
                staleJob.setCompletedAt(Instant.now());
                slicingQueueRepository.save(staleJob);

                currentlyProcessing.remove(staleJob.getId().toString());
                logService.error("SlicingQueueProcessor", "Marked stale job as failed: " + staleJob.getId());
            }

        } catch (Exception e) {
            logService.error("SlicingQueueProcessor", "Error in stale job cleanup: " + e.getMessage());
        }
    }

    private void processJobAsync(SlicingQueue job) {
        new Thread(() -> {
            String jobKey = job.getId().toString();
            try {
                logService.info("SlicingQueueProcessor", "Processing slicing for queue: " + job.getId());
                slicingService.processSlicing(job.getId());
                processedCount.incrementAndGet();
            } catch (Exception e) {
                logService.error("SlicingQueueProcessor", "Failed to process queue: " + job.getId() + " - " + e.getMessage());
                failedCount.incrementAndGet();
            } finally {
                currentlyProcessing.remove(jobKey);
            }
        }, "SlicingThread-" + job.getId()).start();
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        try {
            long queued = slicingQueueRepository.countByStatus(SlicingStatus.QUEUED.getCode());
            long processing = slicingQueueRepository.countByStatus(SlicingStatus.PROCESSING.getCode());

            builder.withDetail("enabled", processingEnabled)
                    .withDetail("queued_jobs", queued)
                    .withDetail("processing_jobs", processing)
                    .withDetail("currently_processing", currentlyProcessing.size())
                    .withDetail("max_concurrent", maxConcurrentJobs)
                    .withDetail("total_processed", processedCount.get())
                    .withDetail("total_failed", failedCount.get())
                    .withDetail("last_processing", lastProcessingTime);

            if (queued > 50) {
                builder.withDetail("warning", "High number of queued jobs: " + queued);
            }

            Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);
            if (lastProcessingTime.isBefore(fiveMinutesAgo) && queued > 0) {
                builder.down().withDetail("error", "No processing activity in last 5 minutes despite queued jobs");
            }

        } catch (Exception e) {
            builder.down().withDetail("error", "Health check failed: " + e.getMessage());
        }

        return builder.build();
    }

    public int getProcessedCount() {
        return processedCount.get();
    }

    public int getFailedCount() {
        return failedCount.get();
    }

    public int getCurrentlyProcessing() {
        return currentlyProcessing.size();
    }

    public Instant getLastProcessingTime() {
        return lastProcessingTime;
    }

    public boolean isProcessingEnabled() {
        return processingEnabled;
    }
}