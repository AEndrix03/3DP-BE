package it.aredegalli.printer.scheduled;

import it.aredegalli.printer.enums.slicing.SlicingStatus;
import it.aredegalli.printer.repository.slicing.SlicingQueueRepository;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.slicing.SlicingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SlicingQueueProcessor {

    private final SlicingQueueRepository slicingQueueRepository;
    private final SlicingService slicingService;
    private final LogService logService;

    @Value("${slicing.max-concurrent:3}")
    private int maxConcurrentSlicing;

    @Scheduled(fixedDelay = 10000) // Check every 10 seconds
    public void processQueue() {
        try {
            // Get queued items
            var queuedItems = slicingQueueRepository.findNextInQueue();

            // Count currently processing
            long processingCount = slicingQueueRepository.countByStatus(SlicingStatus.PROCESSING);

            // Calculate how many we can process
            int availableSlots = (int) (maxConcurrentSlicing - processingCount);
            int toProcess = Math.min(queuedItems.size(), availableSlots);

            if (toProcess > 0) {
                logService.info("SlicingQueueProcessor", "Processing " + toProcess + " slicing jobs");

                for (int i = 0; i < toProcess; i++) {
                    var queue = queuedItems.get(i);
                    logService.info("SlicingQueueProcessor", "Starting slicing for queue: " + queue.getId());
                    slicingService.processSlicing(queue.getId());
                }
            }

        } catch (Exception e) {
            logService.error("SlicingQueueProcessor", "Error in queue processing: " + e.getMessage());
        }
    }
}