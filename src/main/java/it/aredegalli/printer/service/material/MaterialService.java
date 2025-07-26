package it.aredegalli.printer.service.material;

import it.aredegalli.printer.dto.material.MaterialDto;

import java.util.List;
import java.util.UUID;

public interface MaterialService {
    List<MaterialDto> getAllMaterials();

    MaterialDto getMaterialById(UUID id);

    UUID saveMaterial(MaterialDto material);
}
