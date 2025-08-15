package it.aredegalli.printer.service.printer;

import it.aredegalli.common.exception.BadRequestException;
import it.aredegalli.common.exception.NotFoundException;
import it.aredegalli.printer.dto.printer.PrinterCreateDto;
import it.aredegalli.printer.dto.printer.PrinterDto;
import it.aredegalli.printer.dto.printer.PrinterFilterDto;
import it.aredegalli.printer.mapper.printer.PrinterMapper;
import it.aredegalli.printer.model.driver.Driver;
import it.aredegalli.printer.model.printer.Printer;
import it.aredegalli.printer.repository.driver.DriverRepository;
import it.aredegalli.printer.repository.printer.PrinterRepository;
import it.aredegalli.printer.service.log.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrinterServiceImpl implements PrinterService {

    private final PrinterRepository printerRepository;
    private final DriverRepository driverRepository;
    private final LogService log;
    private final PrinterMapper printerMapper;

    @Override
    public PrinterDto getPrinterById(UUID id) {
        return printerRepository.findById(id).map(printerMapper::toDto).orElse(null);
    }

    @Override
    public List<PrinterDto> getAllPrinters() {
        return printerRepository.findAll().stream()
                .map(p -> {
                    PrinterDto _p = PrinterDto.builder()
                            .id(p.getId())
                            .name(p.getName())
                            .image(p.getImage())
                            .status(p.getStatus() != null ? p.getStatus().getCode() : null)
                            .driverId(p.getDriverId())
                            .lastSeen(p.getLastSeen())
                            .build();
                    return _p;
                })
                .toList();
    }

    @Override
    public UUID createPrinter(PrinterCreateDto printerCreateDto) {
        Printer printer = printerRepository.save(Printer.builder()
                .name(printerCreateDto.getName())
                .driverId(printerCreateDto.getDriverid())
                .build());

        log.debug("PrinterServiceImpl", "[API] Created printer: " + printer.getId());

        return printer.getId();
    }

    @Override
    public UUID connectDriverToPrinter(UUID printerId, UUID driverId) {
        Printer printer = printerRepository.findById(printerId).orElseThrow(() -> new NotFoundException("Printer not found"));
        Driver driver = driverRepository.findById(driverId).orElseThrow(() -> new NotFoundException("Driver not found"));

        printer.setDriverId(driver.getId());

        printer = printerRepository.save(printer);
        log.debug("PrinterServiceImpl", "[API] Connected driver " + driver.getId() + " to printer " + printer.getId());
        return printer.getId();
    }

    @Override
    public UUID disconnectDriverFromPrinter(UUID printerId) {
        Printer printer = printerRepository.findById(printerId).orElseThrow(() -> new NotFoundException("Printer not found"));

        printer.setDriverId(null);
        printer = printerRepository.save(printer);

        log.debug("PrinterServiceImpl", "[API] Disconnected driver from printer " + printer.getId());
        return printer.getId();
    }

    @Override
    public PrinterDto getPrinterByName(String name) {
        return printerRepository.findByName(name).map(printerMapper::toDto).orElse(null);
    }

    @Override
    public PrinterDto getPrinterByDriverId(UUID driverId) {
        return printerRepository.findByDriverId(driverId).map(printerMapper::toDto).orElse(null);
    }

    @Override
    public UUID renamePrinter(UUID printerId, String newName) {
        Printer printer = printerRepository.findById(printerId).orElseThrow(() -> new NotFoundException("Printer not found"));

        printer.setName(newName);
        printer = printerRepository.save(printer);
        log.debug("PrinterServiceImpl", "[API] Renamed printer " + printer.getId() + " to " + newName);
        return printer.getId();
    }

    @Override
    public UUID deletePrinter(UUID printerId) {
        Printer printer = printerRepository.findById(printerId).
                orElseThrow(() -> new BadRequestException("Printer not found"));
        printerRepository.delete(printer);

        log.debug("PrinterServiceImpl", "[API] Deleted printer " + printer.getId());
        return printer.getId();
    }

    @Override
    public List<PrinterDto> searchPrinters(PrinterFilterDto filters) {
        log.debug("PrinterServiceImpl", "[API] Searching printers with filters: " + filters);

        // If no filters, return all printers
        if (filters == null || !filters.hasAnyFilter()) {
            return getAllPrinters();
        }

        // Get all printers and apply filtering
        List<PrinterDto> allPrinters = getAllPrinters();

        return allPrinters.stream()
                .filter(printer -> matchesFilters(printer, filters))
                .collect(Collectors.toList());
    }

    @Override
    public List<PrinterDto> getPrintersByStatus(String status) {
        log.debug("PrinterServiceImpl", "[API] Getting printers by status: " + status);

        return printerRepository.findPrinterByStatus_Code(status).stream()
                .map(printerMapper::toDto)
                .toList();
    }

    /**
     * Check if a printer matches the given filters
     */
    private boolean matchesFilters(PrinterDto printer, PrinterFilterDto filters) {
        if (filters.hasNameFilter()) {
            String printerName = printer.getName() != null ? printer.getName().toLowerCase() : "";
            if (!printerName.contains(filters.getNameTrimmed())) {
                return false;
            }
        }

        if (filters.hasDriverIdFilter()) {
            String printerDriverId = printer.getDriverId() != null ? printer.getDriverId().toString() : "";
            if (!printerDriverId.toLowerCase().contains(filters.getDriverIdTrimmed().toLowerCase())) {
                return false;
            }
        }

        if (filters.hasStatusFilter()) {
            String printerStatus = printer.getStatus() != null ? printer.getStatus() : "";
            return filters.getStatusTrimmed().equalsIgnoreCase(printerStatus);
        }

        return true;
    }

}