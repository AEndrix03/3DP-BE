package it.aredegalli.printer.service.material;

import it.aredegalli.printer.dto.material.MaterialDto;
import it.aredegalli.printer.dto.material.MaterialUpdateDto;

import java.util.List;
import java.util.UUID;

public interface MaterialService {
    List<MaterialDto> getAllMaterials();

    List<MaterialDto> getAllMaterialsWithRelations();

    MaterialDto getMaterialById(UUID id);

    UUID saveMaterial(MaterialUpdateDto material);

    UUID deleteMaterial(UUID id);

    List<MaterialDto> getMaterialsByType(UUID typeId);

    List<MaterialDto> getMaterialsByBrand(UUID brandId);

    List<MaterialDto> getMaterialsByTypeName(String typeName);

    List<MaterialDto> getMaterialsByBrandName(String brandName);

    List<MaterialDto> searchMaterials(String name, String type, String brand);

    List<String> getAllMaterialTypeNames();

    List<String> getAllMaterialBrandNames();
}