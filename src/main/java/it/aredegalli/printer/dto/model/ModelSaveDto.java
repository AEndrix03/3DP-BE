package it.aredegalli.printer.dto.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ModelSaveDto {

    @NotNull
    private UUID id;

    @NotNull
    @NotBlank
    private String name;

    private String description;


}
