package it.aredegalli.printer.repository.slicing.result;

import it.aredegalli.printer.model.slicing.result.SlicingResultMaterial;
import it.aredegalli.printer.repository.UUIDRepository;

import java.util.List;
import java.util.UUID;

public interface SlicingResultMaterialRepository extends UUIDRepository<SlicingResultMaterial> {

    List<SlicingResultMaterial> findSlicingResultMaterialBySlicingResultId(UUID slicingResultId);

}
