package it.aredegalli.printer.repository.slicing.property;

import it.aredegalli.printer.model.slicing.property.SlicingProperty;
import it.aredegalli.printer.repository.UUIDRepository;

import java.util.List;

public interface SlicingPropertyRepository extends UUIDRepository<SlicingProperty> {

    List<SlicingProperty> findSlicingPropertiesByCreatedByUserIdOrIsPublicTrue(String createdUserId);

}