package it.aredegalli.printer.dto.printer.detail;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class PrinterDetailSaveDto extends PrinterDetailCreateDto {

    @NotNull(message = "Printer ID is required for update operations")
    private UUID id;
}