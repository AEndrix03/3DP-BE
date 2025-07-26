package it.aredegalli.printer.controller.api;

import it.aredegalli.printer.dto.material.MaterialDto;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.material.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/material")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;
    private final LogService log;

    @GetMapping()
    public ResponseEntity<List<MaterialDto>> getAllMaterials() {
        log.info("MaterialController", "Getting all materials");
        return ResponseEntity.ok(this.materialService.getAllMaterials());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialDto> getMaterialById(@PathVariable UUID id) {
        log.info("MaterialController", "Getting material by id: " + id);
        return ResponseEntity.ok(this.materialService.getMaterialById(id));
    }

    @PatchMapping()
    public ResponseEntity<UUID> saveMaterial(@RequestBody MaterialDto material) {
        log.info("MaterialController", "Save material");
        return ResponseEntity.ok(this.materialService.saveMaterial(material));
    }

}
