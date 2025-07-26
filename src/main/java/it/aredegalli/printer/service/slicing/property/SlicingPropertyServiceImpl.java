package it.aredegalli.printer.service.slicing.property;

import it.aredegalli.printer.dto.slicing.SlicingPropertyDto;
import it.aredegalli.printer.mapper.slicing.SlicingPropertyMapper;
import it.aredegalli.printer.model.slicing.property.SlicingProperty;
import it.aredegalli.printer.model.slicing.property.SlicingPropertyMaterial;
import it.aredegalli.printer.repository.slicing.property.SlicingPropertyMaterialRepository;
import it.aredegalli.printer.repository.slicing.property.SlicingPropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SlicingPropertyServiceImpl implements SlicingPropertyService {

    private final SlicingPropertyRepository slicingPropertyRepository;
    private final SlicingPropertyMaterialRepository slicingPropertyMaterialRepository;

    private final SlicingPropertyMapper slicingPropertyMapper;

    @Override
    public UUID saveSlicingProperty(SlicingPropertyDto propertyDto) {
        SlicingProperty property = this.slicingPropertyRepository.save(
                SlicingProperty.builder()
                        .id(propertyDto.getId())
                        .name(propertyDto.getName())
                        .description(propertyDto.getDescription())
                        .layerHeightMm(propertyDto.getLayerHeightMm())
                        .firstLayerHeightMm(propertyDto.getFirstLayerHeightMm())
                        .lineWidthMm(propertyDto.getLineWidthMm())
                        .printSpeedMmS(propertyDto.getPrintSpeedMmS())
                        .firstLayerSpeedMmS(propertyDto.getFirstLayerSpeedMmS())
                        .travelSpeedMmS(propertyDto.getTravelSpeedMmS())
                        .infillSpeedMmS(propertyDto.getInfillSpeedMmS())
                        .outerWallSpeedMmS(propertyDto.getOuterWallSpeedMmS())
                        .innerWallSpeedMmS(propertyDto.getInnerWallSpeedMmS())
                        .topBottomSpeedMmS(propertyDto.getTopBottomSpeedMmS())
                        .infillPercentage(propertyDto.getInfillPercentage())
                        .infillPattern(propertyDto.getInfillPattern())
                        .perimeterCount(propertyDto.getPerimeterCount())
                        .topSolidLayers(propertyDto.getTopSolidLayers())
                        .bottomSolidLayers(propertyDto.getBottomSolidLayers())
                        .topBottomThicknessMm(propertyDto.getTopBottomThicknessMm())
                        .supportsEnabled(propertyDto.getSupportsEnabled())
                        .supportAngleThreshold(propertyDto.getSupportAngleThreshold())
                        .supportDensityPercentage(propertyDto.getSupportDensityPercentage())
                        .supportPattern(propertyDto.getSupportPattern())
                        .supportZDistanceMm(propertyDto.getSupportZDistanceMm())
                        .adhesionType(propertyDto.getAdhesionType())
                        .brimEnabled(propertyDto.getBrimEnabled())
                        .brimWidthMm(propertyDto.getBrimWidthMm())
                        .fanEnabled(propertyDto.getFanEnabled())
                        .fanSpeedPercentage(propertyDto.getFanSpeedPercentage())
                        .retractionEnabled(propertyDto.getRetractionEnabled())
                        .retractionDistanceMm(propertyDto.getRetractionDistanceMm())
                        .zhopEnabled(propertyDto.getZhopEnabled())
                        .zhopHeightMm(propertyDto.getZhopHeightMm())
                        .extruderTempC(propertyDto.getExtruderTempC())
                        .bedTempC(propertyDto.getBedTempC())
                        .qualityProfile(propertyDto.getQualityProfile())
                        .advancedSettings(propertyDto.getAdvancedSettings() != null ?
                                propertyDto.getAdvancedSettings() : "{}")
                        .slicerId(propertyDto.getSlicerId())
                        .createdByUserId(propertyDto.getCreatedByUserId())
                        .isPublic(propertyDto.getIsPublic() != null ?
                                propertyDto.getIsPublic() : false)
                        .isActive(propertyDto.getIsActive() != null ?
                                propertyDto.getIsActive() : true)
                        .build()
        );

        this.slicingPropertyMaterialRepository.saveAll(propertyDto.getMaterialIds()
                .stream()
                .map(id -> SlicingPropertyMaterial.builder()
                        .slicingPropertyId(property.getId())
                        .materialId(id)
                        .build())
                .toList()
        );

        return property.getId();
    }

    @Override
    public List<SlicingPropertyDto> getSlicingPropertyByUserId(String userId) {
        List<SlicingPropertyDto> dto = this.slicingPropertyMapper.toDtoList(this.slicingPropertyRepository.findSlicingPropertiesByCreatedByUserIdOrIsPublicTrue(userId));

        dto.forEach(d -> {
            d.setMaterialIds(this.slicingPropertyMaterialRepository.findBySlicingPropertyId(d.getId()).stream()
                    .map(SlicingPropertyMaterial::getMaterialId)
                    .toList());
        });

        return dto;
    }

}
