package it.aredegalli.printer.controller.api;

import it.aredegalli.printer.dto.slicing.SlicingResultDto;
import it.aredegalli.printer.dto.slicing.queue.SlicingQueueCreateDto;
import it.aredegalli.printer.dto.slicing.queue.SlicingQueueDto;
import it.aredegalli.printer.dto.slicing.queue.SlicingRequestDto;
import it.aredegalli.printer.enums.slicing.SlicingStatus;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.slicing.SlicingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/slicing")
@RequiredArgsConstructor
public class SlicingController {

    private final SlicingService slicingService;
    private final LogService log;

    // EXISTING ENDPOINTS
    @GetMapping()
    public ResponseEntity<SlicingResultDto> getSlicingResultById(@RequestParam("id") @NotNull UUID id) {
        log.info("SlicingController", "getSlicingResultById with id: " + id);
        return ResponseEntity.ok(slicingService.getSlicingResultById(id));
    }

    @GetMapping("/source")
    public ResponseEntity<List<SlicingResultDto>> getAllSlicingResultBySourceId(@RequestParam("id") @NotNull UUID id) {
        log.info("SlicingController", "getAllSlicingResultBySourceId with source id: " + id);
        return ResponseEntity.ok(slicingService.getAllSlicingResultBySourceId(id));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteSlicingResultById(@RequestParam("id") @NotNull UUID id) {
        log.info("SlicingController", "deleteSlicingResultById with id: " + id);
        slicingService.deleteSlicingResultById(id);
        return ResponseEntity.noContent().build();
    }

    // NEW ENDPOINTS
    @PostMapping("/queue")
    public ResponseEntity<UUID> queueSlicing(@Valid @RequestBody SlicingQueueCreateDto request) {
        log.info("SlicingController", "Queueing slicing request for model: " + request.getModelId());

        UUID queueId = slicingService.queueSlicing(
                request.getModelId(),
                request.getSlicingPropertyId(),
                request.getPriority()
        );

        return ResponseEntity.ok(queueId);
    }

    @GetMapping("/queue/{id}")
    public ResponseEntity<SlicingQueueDto> getQueueStatus(@PathVariable UUID id) {
        var queue = slicingService.getQueueStatus(id);
        if (queue == null) {
            return ResponseEntity.notFound().build();
        }

        SlicingQueueDto dto = SlicingQueueDto.builder()
                .id(queue.getId())
                .modelId(queue.getModel().getId())
                .slicingPropertyId(queue.getSlicingProperty().getId())
                .priority(queue.getPriority())
                .status(SlicingStatus.valueOf(queue.getStatus()))
                .createdAt(queue.getCreatedAt())
                .startedAt(queue.getStartedAt())
                .completedAt(queue.getCompletedAt())
                .errorMessage(queue.getErrorMessage())
                .progressPercentage(queue.getProgressPercentage())
                .build();

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/slice-now")
    public ResponseEntity<SlicingResultDto> sliceImmediately(@Valid @RequestBody SlicingRequestDto request) {
        log.info("SlicingController", "Immediate slicing request");
        // Implementation for direct slicing without queue
        return ResponseEntity.accepted().build();
    }
}