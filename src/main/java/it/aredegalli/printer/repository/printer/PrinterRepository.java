package it.aredegalli.printer.repository.printer;

import it.aredegalli.printer.model.printer.Printer;
import it.aredegalli.printer.repository.UUIDRepository;

import java.util.Optional;
import java.util.UUID;

public interface PrinterRepository extends UUIDRepository<Printer> {

    Optional<Printer> findByName(String name);

    Optional<Printer> findByDriverId(UUID driverId);

}
