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
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Optimized queue processor with monitoring and duplicate prevention
 */
@Service
@RequiredArgsConstructor
public class SlicingQueueProcessor implements HealthIndicator {

    private final SlicingQueueRepository slicingQueueRepository;
    private final SlicingService slicingService;
    private final LogService logService;

    @Value("${slicing.queue.processing.enabled:true}")
    private boolean processingEnabled;

    @Value("${slicing.queue.processing.batch-size:5}")
    private int batchSize;

    @Value("${slicing.queue.processing.max-concurrent:3}")
    private int maxConcurrentJobs;

    @Value("${slicing.queue.stale-timeout-minutes:30}")
    private int staleTimeoutMinutes;

    // Monitoring counters
    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);
    private final AtomicInteger currentlyProcessing = new AtomicInteger(0);
    private Instant lastProcessingTime = Instant.now();

    /**
     * Process queued slicing jobs every 10 seconds
     */
    @Scheduled(fixedDelay = 10000) // 10 seconds
    public void processQueue() {
        if (!processingEnabled) {
            return;
        }

        try {
            // Check current processing load
            long currentProcessingJobs = slicingQueueRepository.countByStatus(SlicingStatus.PROCESSING.getCode());

            if (currentProcessingJobs >= maxConcurrentJobs) {
                logService.debug("SlicingQueueProcessor",
                        String.format("Max concurrent jobs reached (%d/%d), skipping processing",
                                currentProcessingJobs, maxConcurrentJobs));
                return;
            }

            // Get next jobs to process
            List<SlicingQueue> nextJobs = slicingQueueRepository.findNextInQueue();

            if (nextJobs.isEmpty()) {
                return;
            }

            // Process up to remaining capacity
            int availableSlots = (int) (maxConcurrentJobs - currentProcessingJobs);
            int jobsToProcess = Math.min(Math.min(nextJobs.size(), batchSize), availableSlots);

            logService.info("SlicingQueueProcessor",
                    String.format("Processing %d slicing jobs (currently processing: %d/%d)",
                            jobsToProcess, currentProcessingJobs, maxConcurrentJobs));

            for (int i = 0; i < jobsToProcess; i++) {
                SlicingQueue job = nextJobs.get(i);
                processJob(job);
            }

            lastProcessingTime = Instant.now();

        } catch (Exception e) {
            logService.error("SlicingQueueProcessor", "Error in queue processing: " + e.getMessage());
            failedCount.incrementAndGet();
        }
    }

    /**
     * Clean up stale processing jobs every 5 minutes
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
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

            if (!staleJobs.isEmpty()) {
                logService.warn("SlicingQueueProcessor",
                        String.format("Found %d stale processing jobs, marking as failed", staleJobs.size()));

                for (SlicingQueue staleJob : staleJobs) {
                    staleJob.setStatus(SlicingStatus.FAILED.getCode());
                    staleJob.setErrorMessage("Job timed out after " + staleTimeoutMinutes + " minutes");
                    staleJob.setCompletedAt(Instant.now());
                    slicingQueueRepository.save(staleJob);

                    logService.error("SlicingQueueProcessor",
                            String.format("Marked stale job as failed: %s (started: %s)",
                                    staleJob.getId(), staleJob.getStartedAt()));
                }
            }

        } catch (Exception e) {
            logService.error("SlicingQueueProcessor", "Error in stale job cleanup: " + e.getMessage());
        }
    }

    /**
     * Initialize monitoring on application startup
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationStartup() {
        logService.info("SlicingQueueProcessor",
                String.format("Queue processor initialized - enabled: %s, max concurrent: %d, batch size: %d",
                        processingEnabled, maxConcurrentJobs, batchSize));

        // Report current queue status
        reportQueueStatus();
    }

    /**
     * Report queue statistics every hour
     */
    @Scheduled(fixedDelay = 3600000) // 1 hour
    public void reportQueueStatus() {
        try {
            long queued = slicingQueueRepository.countByStatus(SlicingStatus.QUEUED.getCode());
            long processing = slicingQueueRepository.countByStatus(SlicingStatus.PROCESSING.getCode());
            long completed = slicingQueueRepository.countByStatus(SlicingStatus.COMPLETED.getCode());
            long failed = slicingQueueRepository.countByStatus(SlicingStatus.FAILED.getCode());

            logService.info("SlicingQueueProcessor",
                    String.format("Queue status - Queued: %d, Processing: %d, Completed: %d, Failed: %d | " +
                                    "Total processed: %d, Total failed: %d",
                            queued, processing, completed, failed,
                            processedCount.get(), failedCount.get()));

        } catch (Exception e) {
            logService.error("SlicingQueueProcessor", "Error reporting queue status: " + e.getMessage());
        }
    }

    private void processJob(SlicingQueue job) {
        try {
            logService.info("SlicingQueueProcessor",
                    String.format("Starting slicing for queue: %s (model: %s, priority: %d)",
                            job.getId(), job.getModel().getName(), job.getPriority()));

            currentlyProcessing.incrementAndGet();
            slicingService.processSlicing(job.getId());
            processedCount.incrementAndGet();

        } catch (Exception e) {
            logService.error("SlicingQueueProcessor",
                    String.format("Failed to start slicing for queue: %s - %s", job.getId(), e.getMessage()));
            failedCount.incrementAndGet();
        } finally {
            currentlyProcessing.decrementAndGet();
        }
    }

    /**
     * Health check for the queue processor
     */
    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        try {
            long queued = slicingQueueRepository.countByStatus(SlicingStatus.QUEUED.getCode());
            long processing = slicingQueueRepository.countByStatus(SlicingStatus.PROCESSING.getCode());

            builder.withDetail("enabled", processingEnabled)
                    .withDetail("queued_jobs", queued)
                    .withDetail("processing_jobs", processing)
                    .withDetail("currently_processing", currentlyProcessing.get())
                    .withDetail("max_concurrent", maxConcurrentJobs)
                    .withDetail("total_processed", processedCount.get())
                    .withDetail("total_failed", failedCount.get())
                    .withDetail("last_processing", lastProcessingTime);

            // Health warnings
            if (processing > maxConcurrentJobs) {
                builder.withDetail("warning", "More jobs processing than max concurrent limit");
            }

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

    // Getter methods for monitoring
    public int getProcessedCount() {
        return processedCount.get();
    }

    public int getFailedCount() {
        return failedCount.get();
    }

    public int getCurrentlyProcessing() {
        return currentlyProcessing.get();
    }

    public Instant getLastProcessingTime() {
        return lastProcessingTime;
    }

    public boolean isProcessingEnabled() {
        return processingEnabled;
    }
}