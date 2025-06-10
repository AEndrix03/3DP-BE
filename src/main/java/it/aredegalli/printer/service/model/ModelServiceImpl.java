package it.aredegalli.printer.service.model;

import it.aredegalli.common.exception.NotFoundException;
import it.aredegalli.printer.dto.model.ModelDto;
import it.aredegalli.printer.dto.model.ModelSaveDto;
import it.aredegalli.printer.mapper.model.ModelMapper;
import it.aredegalli.printer.model.model.Model;
import it.aredegalli.printer.repository.model.ModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModelServiceImpl implements ModelService {

    private final ModelRepository modelRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<ModelDto> getAllModels() {
        return modelRepository.findAll()
                .stream()
                .map(modelMapper::toDto)
                .toList();
    }

    @Override
    public ModelDto getModelById(UUID id) {
        return modelRepository.findById(id)
                .map(modelMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Model not found with id: " + id));
    }

    @Override
    public UUID saveModel(ModelSaveDto modelSaveDto) {
        Model model = modelRepository.findById(modelSaveDto.getId()).orElseThrow(() -> new NotFoundException("Model not found with id: " + modelSaveDto.getId()));

        model.setName(modelSaveDto.getName());
        model.setDescription(modelSaveDto.getDescription());

        return modelRepository.save(model).getId();
    }

}
