package it.aredegalli.printer.service.material.brand;

import it.aredegalli.common.exception.NotFoundException;
import it.aredegalli.printer.dto.material.MaterialBrandDto;
import it.aredegalli.printer.mapper.material.MaterialBrandMapper;
import it.aredegalli.printer.model.material.MaterialBrand;
import it.aredegalli.printer.repository.material.MaterialBrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaterialBrandServiceImpl implements MaterialBrandService {

    private final MaterialBrandRepository materialBrandRepository;
    private final MaterialBrandMapper materialBrandMapper;

    @Override
    public List<MaterialBrandDto> getAllMaterialBrands() {
        var materialBrands = this.materialBrandRepository.findAll();
        return this.materialBrandMapper.toDto(materialBrands);
    }

    @Override
    public List<MaterialBrandDto> getActiveMaterialBrands() {
        var materialBrands = this.materialBrandRepository.findByIsActiveTrue();
        return this.materialBrandMapper.toDto(materialBrands);
    }

    @Override
    public MaterialBrandDto getMaterialBrandById(UUID id) {
        return this.materialBrandMapper.toDto(
                this.materialBrandRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("Material brand not found"))
        );
    }

    @Override
    public MaterialBrandDto getMaterialBrandByName(String name) {
        return this.materialBrandMapper.toDto(
                this.materialBrandRepository.findByNameAndIsActiveTrue(name)
                        .orElseThrow(() -> new NotFoundException("Material brand not found"))
        );
    }

    @Override
    public UUID saveMaterialBrand(MaterialBrandDto materialBrand) {
        MaterialBrand _materialBrand = this.materialBrandMapper.toEntity(materialBrand);
        return this.materialBrandRepository.save(_materialBrand).getId();
    }

    @Override
    public UUID deleteMaterialBrand(UUID id) {
        this.materialBrandRepository.deleteById(id);
        return id;
    }

    @Override
    public List<MaterialBrandDto> getHighQualityBrands(Integer minRating) {
        var materialBrands = this.materialBrandRepository.findByMinQualityRating(minRating);
        return this.materialBrandMapper.toDto(materialBrands);
    }
}