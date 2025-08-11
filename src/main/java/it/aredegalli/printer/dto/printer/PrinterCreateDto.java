package it.aredegalli.printer.dto.printer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class PrinterCreateDto {
    @NotBlank
    private String name;

    private UUID driverid;
}
