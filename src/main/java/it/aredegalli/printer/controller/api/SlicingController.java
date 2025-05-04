package it.aredegalli.printer.controller.api;

import it.aredegalli.printer.dto.slicing.SlicingResultDto;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.printer.SlicingService;
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

    @GetMapping("/{id}")
    public ResponseEntity<SlicingResultDto> getSlicingResultById(@PathVariable UUID id) {
        log.info("SlicingController", "getSlicingResultById with id: " + id);
        return ResponseEntity.ok(slicingService.getSlicingResultById(id));
    }

    @GetMapping("/source/{sourceId}")
    public ResponseEntity<List<SlicingResultDto>> getAllSlicingResultBySourceId(@PathVariable UUID sourceId) {
        log.info("SlicingController", "getAllSlicingResultBySourceId with source id: " + sourceId);
        return ResponseEntity.ok(slicingService.getAllSlicingResultBySourceId(sourceId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSlicingResultById(@PathVariable UUID id) {
        log.info("SlicingController", "deleteSlicingResultById with id: " + id);
        slicingService.deleteSlicingResultById(id);
        return ResponseEntity.noContent().build();
    }

}
