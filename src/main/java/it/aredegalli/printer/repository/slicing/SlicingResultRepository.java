package it.aredegalli.printer.repository.slicing;

import it.aredegalli.printer.model.slicing.SlicingResult;
import it.aredegalli.printer.repository.UUIDRepository;

import java.util.List;
import java.util.UUID;

public interface SlicingResultRepository extends UUIDRepository<SlicingResult> {

    List<SlicingResult> findBySourceFile_Id(UUID sourceFileId);

}
