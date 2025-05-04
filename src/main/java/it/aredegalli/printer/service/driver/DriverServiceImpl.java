package it.aredegalli.printer.service.driver;

import it.aredegalli.common.exception.NotFoundException;
import it.aredegalli.printer.dto.driver.DriverDto;
import it.aredegalli.printer.mapper.driver.DriverMapper;
import it.aredegalli.printer.model.driver.Driver;
import it.aredegalli.printer.repository.driver.DriverRepository;
import it.aredegalli.printer.service.log.LogService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final DriverMapper driverMapper;
    private final LogService log;

    @Override
    public DriverDto getDriverById(UUID id) {
        return driverRepository.findById(id)
                .map(driverMapper::toDto)
                .orElse(null);
    }

    @Override
    public List<DriverDto> getAllDrivers() {
        return driverRepository.findAll()
                .stream()
                .map(driverMapper::toDto)
                .toList();
    }

    @Override
    public UUID createDriver(@NotNull String publicKey) {
        Driver driver = driverRepository.save(Driver.builder()
                .publicKey(publicKey)
                .build());

        log.debug("DriverServiceImpl", "Created driver with id: " + driver.getId());
        return driver.getId();
    }

    @Override
    public UUID updateDriver(UUID id, @NotNull String publicKey) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Driver not found"));

        driver.setPublicKey(publicKey);
        driverRepository.save(driver);

        log.debug("DriverServiceImpl", "Updated driver with id: " + driver.getId());
        return driver.getId();
    }

    @Override
    public void deleteDriver(UUID id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Driver not found"));

        driverRepository.delete(driver);
        log.debug("DriverServiceImpl", "Deleted driver with id: " + driver.getId());
    }

}
