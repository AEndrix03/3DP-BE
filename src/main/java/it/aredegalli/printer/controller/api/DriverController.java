package it.aredegalli.printer.controller.api;

import it.aredegalli.printer.dto.driver.DriverCreateDto;
import it.aredegalli.printer.dto.driver.DriverDto;
import it.aredegalli.printer.dto.driver.DriverSaveDto;
import it.aredegalli.printer.service.driver.DriverService;
import it.aredegalli.printer.service.log.LogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;
    private final LogService log;

    @GetMapping("/{id}")
    public ResponseEntity<DriverDto> getDriverById(@PathVariable UUID id) {
        log.info("DriverController", "getDriverById with id: " + id);
        return ResponseEntity.ok(driverService.getDriverById(id));
    }

    @GetMapping()
    public ResponseEntity<List<DriverDto>> getAllDrivers() {
        log.info("DriverController", "getAllDrivers");
        return ResponseEntity.ok(driverService.getAllDrivers());
    }

    @PostMapping()
    public ResponseEntity<UUID> createDriver(@Valid @RequestBody DriverCreateDto driverCreateDto) {
        log.info("DriverController", "createDriver");
        return ResponseEntity.ok(driverService.createDriver(driverCreateDto.getPublicKey()));
    }

    @PutMapping()
    public ResponseEntity<UUID> updateDriver(@Valid @RequestBody DriverSaveDto driverSaveDto) {
        log.info("DriverController", "updateDriver");
        return ResponseEntity.ok(driverService.updateDriver(driverSaveDto.getId(), driverSaveDto.getPublicKey()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDriver(@PathVariable UUID id) {
        log.info("DriverController", "deleteDriver with id: " + id);
        driverService.deleteDriver(id);
        return ResponseEntity.noContent().build();
    }

}
