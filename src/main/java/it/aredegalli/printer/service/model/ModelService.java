package it.aredegalli.printer.service.model;

import it.aredegalli.printer.dto.model.ModelDto;
import it.aredegalli.printer.dto.model.ModelSaveDto;

import java.util.List;
import java.util.UUID;

public interface ModelService {
    List<ModelDto> getAllModels();

    ModelDto getModelById(UUID id);

    UUID saveModel(ModelSaveDto modelSaveDto);
}
