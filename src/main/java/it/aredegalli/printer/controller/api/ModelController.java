package it.aredegalli.printer.controller.api;

import it.aredegalli.printer.dto.model.ModelDto;
import it.aredegalli.printer.dto.model.ModelSaveDto;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.model.ModelService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/model")
public class ModelController {

    private final ModelService modelService;
    private final LogService log;

    @GetMapping("/all")
    public ResponseEntity<List<ModelDto>> getAllModels() {
        log.info("ModelController", "Listing all uploaded models");
        return ResponseEntity.ok(modelService.getAllModels());
    }

    @GetMapping
    public ResponseEntity<ModelDto> getModelById(@RequestParam("id") @NotNull UUID id) {
        log.info("ModelController", "Getting model by ID: " + id);
        return ResponseEntity.ok(modelService.getModelById(id));
    }

    @PatchMapping
    public ResponseEntity<UUID> saveModel(@RequestBody @Valid @NotNull ModelSaveDto modelSaveDto) {
        log.info("ModelController", "Saving model with ID: " + modelSaveDto.getId());
        UUID savedModelId = modelService.saveModel(modelSaveDto);
        return ResponseEntity.ok(savedModelId);
    }
}
