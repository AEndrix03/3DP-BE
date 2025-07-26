package it.aredegalli.printer.controller.api.slicing;

import it.aredegalli.printer.dto.slicing.SlicingPropertyDto;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.slicing.property.SlicingPropertyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/slicing/property")
@RequiredArgsConstructor
public class SlicingPropertyController {

    private final LogService log;

    private final SlicingPropertyService slicingPropertyService;

    @GetMapping()
    public ResponseEntity<List<SlicingPropertyDto>> getSlicingPropertyByUserId(@RequestParam String userId) {
        log.info("SlicingPropertyController", "Get SlicingProperty  by User Id: " + userId);
        return ResponseEntity.ok(this.slicingPropertyService.getSlicingPropertyByUserId(userId));
    }

    @PatchMapping()
    public ResponseEntity<UUID> saveSlicingProperty(@RequestBody SlicingPropertyDto dto) {
        log.info("SlicingPropertyController", "Patching Slicing Property");
        return ResponseEntity.ok(this.slicingPropertyService.saveSlicingProperty(dto));
    }
}