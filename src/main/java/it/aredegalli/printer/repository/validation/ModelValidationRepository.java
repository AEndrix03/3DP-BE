package it.aredegalli.printer.repository.validation;

import it.aredegalli.printer.model.validation.ModelValidation;
import it.aredegalli.printer.repository.UUIDRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModelValidationRepository extends UUIDRepository<ModelValidation> {

    Optional<ModelValidation> findByModelId(UUID modelId);

    List<ModelValidation> findByHasErrorsTrue();

    List<ModelValidation> findByIsManifoldFalse();
}