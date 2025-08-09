package it.aredegalli.printer.service.material.type;

import it.aredegalli.common.exception.NotFoundException;
import it.aredegalli.printer.dto.material.MaterialTypeDto;
import it.aredegalli.printer.mapper.material.MaterialTypeMapper;
import it.aredegalli.printer.model.material.MaterialType;
import it.aredegalli.printer.repository.material.MaterialTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaterialTypeServiceImpl implements MaterialTypeService {

    private final MaterialTypeRepository materialTypeRepository;
    private final MaterialTypeMapper materialTypeMapper;

    @Override
    public List<MaterialTypeDto> getAllMaterialTypes() {
        var materialTypes = this.materialTypeRepository.findAll();
        return this.materialTypeMapper.toDto(materialTypes);
    }

    @Override
    public List<MaterialTypeDto> getActiveMaterialTypes() {
        var materialTypes = this.materialTypeRepository.findByIsActiveTrue();
        return this.materialTypeMapper.toDto(materialTypes);
    }

    @Override
    public MaterialTypeDto getMaterialTypeById(UUID id) {
        return this.materialTypeMapper.toDto(
                this.materialTypeRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("Material type not found"))
        );
    }

    @Override
    public MaterialTypeDto getMaterialTypeByName(String name) {
        return this.materialTypeMapper.toDto(
                this.materialTypeRepository.findByNameAndIsActiveTrue(name)
                        .orElseThrow(() -> new NotFoundException("Material type not found"))
        );
    }

    @Override
    public UUID saveMaterialType(MaterialTypeDto materialType) {
        MaterialType _materialType = this.materialTypeMapper.toEntity(materialType);
        return this.materialTypeRepository.save(_materialType).getId();
    }

    @Override
    public UUID deleteMaterialType(UUID id) {
        this.materialTypeRepository.deleteById(id);
        return id;
    }

    @Override
    public List<MaterialTypeDto> getFlexibleMaterialTypes() {
        var materialTypes = this.materialTypeRepository.findByIsFlexibleAndActive(true);
        return this.materialTypeMapper.toDto(materialTypes);
    }
}