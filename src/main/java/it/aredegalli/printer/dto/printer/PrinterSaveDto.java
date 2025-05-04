package it.aredegalli.printer.dto.printer;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class PrinterSaveDto extends PrinterCreateDto {
    @NotNull
    private UUID id;
}
