package it.aredegalli.printer.service.material;

import it.aredegalli.common.exception.NotFoundException;
import it.aredegalli.printer.dto.material.MaterialDto;
import it.aredegalli.printer.mapper.material.MaterialMapper;
import it.aredegalli.printer.model.material.Material;
import it.aredegalli.printer.repository.material.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialMapper materialMapper;

    @Override()
    public List<MaterialDto> getAllMaterials() {
        var materials = this.materialRepository.findAll();
        return this.materialMapper.toDto(materials);
    }

    @Override()
    public MaterialDto getMaterialById(UUID id) {
        return this.materialMapper.toDto(this.materialRepository.findById(id).orElseThrow(() -> new NotFoundException("Material not found")));
    }

    @Override()
    public UUID saveMaterial(MaterialDto material) {
        Material _material = this.materialMapper.toEntity(material);
        return this.materialRepository.save(_material).getId();
    }
}
