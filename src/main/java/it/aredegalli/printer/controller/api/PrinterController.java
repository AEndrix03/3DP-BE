package it.aredegalli.printer.controller.api;

import it.aredegalli.printer.dto.printer.PrinterCreateDto;
import it.aredegalli.printer.dto.printer.PrinterDto;
import it.aredegalli.printer.dto.printer.PrinterSaveDto;
import it.aredegalli.printer.dto.printer.detail.PrinterDetailDto;
import it.aredegalli.printer.dto.printer.detail.PrinterDetailSaveDto;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.printer.PrinterService;
import it.aredegalli.printer.service.printer.detail.PrinterDetailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/printer")
@RequiredArgsConstructor
public class PrinterController {

    private final PrinterService printerService;
    private final PrinterDetailService printerDetailService;
    private final LogService log;

    @GetMapping("/{id}")
    public ResponseEntity<PrinterDto> getPrinterById(@PathVariable UUID id) {
        log.info("PrinterController", "getPrinterById with id: " + id);
        return ResponseEntity.ok(printerService.getPrinterById(id));
    }

    @GetMapping
    public ResponseEntity<List<PrinterDto>> getAllPrinters() {
        log.info("PrinterController", "getAllPrinters");
        return ResponseEntity.ok(printerService.getAllPrinters());
    }

    @PostMapping
    public ResponseEntity<UUID> createPrinter(@Valid @RequestBody PrinterCreateDto printerCreateDto) {
        log.info("PrinterController", "createPrinter");
        return ResponseEntity.ok(printerService.createPrinter(printerCreateDto.getName()));
    }

    @PutMapping
    public ResponseEntity<UUID> updatePrinter(@Valid @RequestBody PrinterSaveDto printerSaveDto) {
        log.info("PrinterController", "updatePrinter");
        return ResponseEntity.ok(printerService.renamePrinter(printerSaveDto.getId(), printerSaveDto.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrinter(@PathVariable UUID id) {
        log.info("PrinterController", "deletePrinter with id: " + id);
        printerService.deletePrinter(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{printerId}/driver/{driverId}")
    public ResponseEntity<UUID> connectDriverToPrinter(@PathVariable UUID printerId, @PathVariable UUID driverId) {
        log.info("PrinterController", "connectDriverToPrinter printerId=" + printerId + ", driverId=" + driverId);
        return ResponseEntity.ok(printerService.connectDriverToPrinter(printerId, driverId));
    }

    @PatchMapping("/{printerId}/driver")
    public ResponseEntity<UUID> disconnectDriverFromPrinter(@PathVariable UUID printerId) {
        log.info("PrinterController", "disconnectDriverFromPrinter printerId=" + printerId);
        return ResponseEntity.ok(printerService.disconnectDriverFromPrinter(printerId));
    }

    @GetMapping("/detail/{printerId}")
    public ResponseEntity<PrinterDetailDto> getPrinterDetail(@PathVariable UUID printerId) {
        log.info("PrinterController", "disconnectDriverFromPrinter printerId=" + printerId);
        return ResponseEntity.ok(printerDetailService.getPrinterById(printerId));
    }

    @PatchMapping("/detail/{printerId}")
    public ResponseEntity<UUID> updatePrinterDetail(@RequestBody PrinterDetailSaveDto printerDetailSaveDto) {
        log.info("PrinterController", "updatePrinterDetail");
        return ResponseEntity.ok(printerDetailService.savePrinter(printerDetailSaveDto));
    }
    
}
