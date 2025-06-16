package it.aredegalli.printer.controller.api;

import it.aredegalli.printer.dto.slicing.SlicingResultDto;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.slicing.SlicingService;
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

}
