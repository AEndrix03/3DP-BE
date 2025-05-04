package it.aredegalli.printer.dto.printer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PrinterCreateDto {
    @NotBlank
    private String name;
}
