package it.aredegalli.printer.controller.api;

import it.aredegalli.printer.dto.material.MaterialDto;
import it.aredegalli.printer.dto.material.MaterialUpdateDto;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.material.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
        return ResponseEntity.ok(this.materialService.getAllMaterialsWithRelations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialDto> getMaterialById(@PathVariable UUID id) {
        log.info("MaterialController", "Getting material by id: " + id);
        return ResponseEntity.ok(this.materialService.getMaterialById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<MaterialDto>> searchMaterials(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String brand) {
        log.info("MaterialController", "Searching materials with filters - name: " + name + ", type: " + type + ", brand: " + brand);
        return ResponseEntity.ok(this.materialService.searchMaterials(name, type, brand));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<MaterialDto>> getMaterialsByType(@PathVariable String type) {
        log.info("MaterialController", "Getting materials by type: " + type);
        return ResponseEntity.ok(this.materialService.getMaterialsByTypeName(type));
    }

    @GetMapping("/brand/{brand}")
    public ResponseEntity<List<MaterialDto>> getMaterialsByBrand(@PathVariable String brand) {
        log.info("MaterialController", "Getting materials by brand: " + brand);
        return ResponseEntity.ok(this.materialService.getMaterialsByBrandName(brand));
    }

    @GetMapping("/types")
    public ResponseEntity<List<String>> getMaterialTypes() {
        log.info("MaterialController", "Getting all material types");
        return ResponseEntity.ok(this.materialService.getAllMaterialTypeNames());
    }

    @GetMapping("/brands")
    public ResponseEntity<List<String>> getMaterialBrands() {
        log.info("MaterialController", "Getting all material brands");
        return ResponseEntity.ok(this.materialService.getAllMaterialBrandNames());
    }

    @PatchMapping()
    public ResponseEntity<UUID> saveMaterial(@RequestBody MaterialUpdateDto material) {
        log.info("MaterialController", "Save material: " + material.getName());
        try {
            UUID id = this.materialService.saveMaterial(material);
            log.info("MaterialController", "Material saved successfully with ID: " + id);
            return ResponseEntity.ok(id);
        } catch (IllegalArgumentException e) {
            log.error("MaterialController", "Invalid material data: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("MaterialController", "Failed to save material: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<UUID> deleteMaterial(@PathVariable UUID id) {
        log.info("MaterialController", "Delete material by id: " + id);
        try {
            UUID deletedId = this.materialService.deleteMaterial(id);
            log.info("MaterialController", "Material deleted successfully: " + deletedId);
            return ResponseEntity.ok(deletedId);
        } catch (Exception e) {
            log.error("MaterialController", "Failed to delete material: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}