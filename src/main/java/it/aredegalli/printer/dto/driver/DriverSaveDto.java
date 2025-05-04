package it.aredegalli.printer.dto.driver;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class DriverSaveDto extends DriverCreateDto {

    @NotNull
    private UUID id;

}
