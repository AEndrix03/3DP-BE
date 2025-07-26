package it.aredegalli.printer.repository.slicing.property;

import it.aredegalli.printer.model.slicing.property.SlicingPropertyMaterial;
import it.aredegalli.printer.repository.UUIDRepository;

import java.util.List;
import java.util.UUID;

public interface SlicingPropertyMaterialRepository extends UUIDRepository<SlicingPropertyMaterial> {

    List<SlicingPropertyMaterial> findBySlicingPropertyId(UUID id);

}