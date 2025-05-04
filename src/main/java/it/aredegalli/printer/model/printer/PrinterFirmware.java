package it.aredegalli.printer.model.printer;

import it.aredegalli.printer.model.driver.FirmwareVersion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "printer_firmware")
public class PrinterFirmware {
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "printer_id")
    private Printer printer;

    @ManyToOne
    @JoinColumn(name = "firmware_id")
    private FirmwareVersion firmwareVersion;

    private Instant installedAt;
}
