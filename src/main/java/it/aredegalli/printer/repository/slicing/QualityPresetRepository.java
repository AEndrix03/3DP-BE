package it.aredegalli.printer.repository.slicing;

import it.aredegalli.printer.model.slicing.QualityPreset;
import it.aredegalli.printer.repository.UUIDRepository;

import java.util.List;

public interface QualityPresetRepository extends UUIDRepository<QualityPreset> {

    List<QualityPreset> findAllByOrderByQualityLevelAsc();

    List<QualityPreset> findByQualityLevelBetween(Integer minLevel, Integer maxLevel);
}