package it.aredegalli.printer.service.slicing.property;

import it.aredegalli.printer.dto.slicing.SlicingPropertyDto;

import java.util.List;
import java.util.UUID;

public interface SlicingPropertyService {
    UUID saveSlicingProperty(SlicingPropertyDto propertyDto);

    List<SlicingPropertyDto> getSlicingPropertyByUserId(String userId);
}
