package it.aredegalli.printer.service.material.type;

import it.aredegalli.printer.dto.material.MaterialTypeDto;

import java.util.List;
import java.util.UUID;

public interface MaterialTypeService {
    List<MaterialTypeDto> getAllMaterialTypes();

    List<MaterialTypeDto> getActiveMaterialTypes();

    MaterialTypeDto getMaterialTypeById(UUID id);

    MaterialTypeDto getMaterialTypeByName(String name);

    UUID saveMaterialType(MaterialTypeDto materialType);

    UUID deleteMaterialType(UUID id);

    List<MaterialTypeDto> getFlexibleMaterialTypes();
}