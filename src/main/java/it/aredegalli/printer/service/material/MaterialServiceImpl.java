package it.aredegalli.printer.service.material;

import it.aredegalli.common.exception.NotFoundException;
import it.aredegalli.printer.dto.material.MaterialDto;
import it.aredegalli.printer.dto.material.MaterialUpdateDto;
import it.aredegalli.printer.mapper.material.MaterialMapper;
import it.aredegalli.printer.model.material.Material;
import it.aredegalli.printer.model.material.MaterialBrand;
import it.aredegalli.printer.model.material.MaterialType;
import it.aredegalli.printer.repository.material.MaterialBrandRepository;
import it.aredegalli.printer.repository.material.MaterialRepository;
import it.aredegalli.printer.repository.material.MaterialTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialTypeRepository materialTypeRepository;
    private final MaterialBrandRepository materialBrandRepository;
    private final MaterialMapper materialMapper;

    @Override
    @Transactional(readOnly = true)
    public List<MaterialDto> getAllMaterials() {
        var materials = this.materialRepository.findAll();
        return this.materialMapper.toDto(materials);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialDto> getAllMaterialsWithRelations() {
        var materials = this.materialRepository.findAllWithTypeAndBrand();
        return this.materialMapper.toDto(materials);
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialDto getMaterialById(UUID id) {
        return this.materialMapper.toDto(
                this.materialRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("Material not found"))
        );
    }

    @Override
    public UUID saveMaterial(MaterialUpdateDto materialUpdate) {
        // Manual mapping from MaterialUpdateDto to Material entity
        Material material = mapUpdateDtoToEntity(materialUpdate);
        Material savedMaterial = this.materialRepository.save(material);
        return savedMaterial.getId();
    }

    @Override
    public UUID deleteMaterial(UUID id) {
        if (!this.materialRepository.existsById(id)) {
            throw new NotFoundException("Material not found");
        }
        this.materialRepository.deleteById(id);
        return id;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialDto> getMaterialsByType(UUID typeId) {
        var type = this.materialTypeRepository.findById(typeId)
                .orElseThrow(() -> new NotFoundException("Material type not found"));
        var materials = this.materialRepository.findByType(type);
        return this.materialMapper.toDto(materials);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialDto> getMaterialsByBrand(UUID brandId) {
        var brand = this.materialBrandRepository.findById(brandId)
                .orElseThrow(() -> new NotFoundException("Material brand not found"));
        var materials = this.materialRepository.findByBrand(brand);
        return this.materialMapper.toDto(materials);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialDto> getMaterialsByTypeName(String typeName) {
        var materials = this.materialRepository.findByTypeNameWithRelations(typeName);
        return this.materialMapper.toDto(materials);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialDto> getMaterialsByBrandName(String brandName) {
        var materials = this.materialRepository.findByBrandNameWithRelations(brandName);
        return this.materialMapper.toDto(materials);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialDto> searchMaterials(String name, String type, String brand) {
        var materials = this.materialRepository.searchMaterials(name, type, brand);
        return this.materialMapper.toDto(materials);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllMaterialTypeNames() {
        return this.materialTypeRepository.findByIsActiveTrue()
                .stream()
                .map(MaterialType::getName)
                .sorted()
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllMaterialBrandNames() {
        return this.materialBrandRepository.findByIsActiveTrue()
                .stream()
                .map(MaterialBrand::getName)
                .sorted()
                .distinct()
                .collect(Collectors.toList());
    }

    // ========== MANUAL MAPPING METHOD ==========

    private Material mapUpdateDtoToEntity(MaterialUpdateDto dto) {
        Material material = new Material();

        // Set ID if provided (for updates)
        if (dto.getId() != null && !dto.getId().trim().isEmpty()) {
            try {
                material.setId(UUID.fromString(dto.getId()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid material ID format: " + dto.getId());
            }
        }

        // Basic fields
        material.setName(dto.getName());
        material.setDensityGCm3(dto.getDensityGCm3());
        material.setDiameterMm(dto.getDiameterMm());
        material.setCostPerKg(dto.getCostPerKg());
        material.setRecommendedExtruderTempMinC(dto.getRecommendedExtruderTempMinC());
        material.setRecommendedExtruderTempMaxC(dto.getRecommendedExtruderTempMaxC());
        material.setRecommendedBedTempC(dto.getRecommendedBedTempC());
        material.setRequiresHeatedBed(dto.getRequiresHeatedBed());
        material.setRequiresChamberHeating(dto.getRequiresChamberHeating());
        material.setSupportsSoluble(dto.getSupportsSoluble());
        material.setShrinkageFactor(dto.getShrinkageFactor());
        material.setImage(dto.getImage());

        // Resolve MaterialType
        String typeToFind = getTypeToFind(dto);
        if (typeToFind != null && !typeToFind.trim().isEmpty()) {
            MaterialType materialType = materialTypeRepository.findByNameAndIsActiveTrue(typeToFind.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Material type '" + typeToFind + "' not found or inactive"));
            material.setType(materialType);
        } else {
            throw new IllegalArgumentException("Material type is required");
        }

        // Resolve MaterialBrand
        String brandToFind = getBrandToFind(dto);
        if (brandToFind != null && !brandToFind.trim().isEmpty()) {
            MaterialBrand materialBrand = materialBrandRepository.findByNameAndIsActiveTrue(brandToFind.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Material brand '" + brandToFind + "' not found or inactive"));
            material.setBrand(materialBrand);
        } else {
            throw new IllegalArgumentException("Material brand is required");
        }

        return material;
    }

    private String getTypeToFind(MaterialUpdateDto dto) {
        if (dto.getType() != null && !dto.getType().trim().isEmpty()) {
            return dto.getType();
        }
        return null;
    }

    private String getBrandToFind(MaterialUpdateDto dto) {
        if (dto.getBrand() != null && !dto.getBrand().trim().isEmpty()) {
            return dto.getBrand();
        }
        return null;
    }
}
