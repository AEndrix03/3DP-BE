package it.aredegalli.printer.dto.driver;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DriverCreateDto {

    @NotBlank
    private String publicKey;

}
