package it.aredegalli.printer.service.driver;

import it.aredegalli.printer.dto.driver.DriverDto;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public interface DriverService {
    DriverDto getDriverById(UUID id);

    UUID createDriver(@NotNull String publicKey);

    UUID updateDriver(UUID id, @NotNull String publicKey);

    void deleteDriver(UUID id);
}
