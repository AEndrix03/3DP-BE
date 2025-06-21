package it.aredegalli.printer.repository.slicing;

import it.aredegalli.printer.model.slicing.SlicingResultMaterial;
import it.aredegalli.printer.repository.UUIDRepository;

import java.util.List;
import java.util.UUID;

public interface SlicingResultMaterialRepository extends UUIDRepository<SlicingResultMaterial> {

    List<SlicingResultMaterial> findSlicingResultMaterialBySlicingResultId(UUID slicingResultId);

}
