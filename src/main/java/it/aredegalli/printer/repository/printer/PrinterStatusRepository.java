package it.aredegalli.printer.repository.printer;

import it.aredegalli.printer.model.printer.PrinterStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrinterStatusRepository extends JpaRepository<PrinterStatus, String> {

}
