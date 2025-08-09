package it.aredegalli.printer.service.material.brand;

import it.aredegalli.printer.dto.material.MaterialBrandDto;

import java.util.List;
import java.util.UUID;

public interface MaterialBrandService {
    List<MaterialBrandDto> getAllMaterialBrands();

    List<MaterialBrandDto> getActiveMaterialBrands();

    MaterialBrandDto getMaterialBrandById(UUID id);

    MaterialBrandDto getMaterialBrandByName(String name);

    UUID saveMaterialBrand(MaterialBrandDto materialBrand);

    UUID deleteMaterialBrand(UUID id);

    List<MaterialBrandDto> getHighQualityBrands(Integer minRating);
}