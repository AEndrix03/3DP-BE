package it.aredegalli.printer.service.slicing.engine;

import it.aredegalli.printer.model.model.Model;
import it.aredegalli.printer.model.slicing.SlicingProperty;
import it.aredegalli.printer.model.slicing.SlicingResult;

public interface SlicingEngine {

    SlicingResult slice(Model model, SlicingProperty properties);

    boolean validateModel(Model model);

    String getName();

    String getVersion();
}