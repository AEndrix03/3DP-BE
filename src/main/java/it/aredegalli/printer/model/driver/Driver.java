package it.aredegalli.printer.model.driver;

import it.aredegalli.printer.model.printer.Printer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "driver")
public class Driver {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "printer_id")
    private Printer printer;

    private Instant lastAuth;

    @Column(name = "custom_start_gcode")
    private String customStartGCode;

    @Column(name = "custom_end_gcode")
    private String customEndGCode;

    @Lob
    private String publicKey;
}
