package it.aredegalli.printer.model.driver;

import it.aredegalli.printer.model.printer.Printer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "driver")
public class Driver {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "printer_id")
    private Printer printer;

    private Instant lastAuth;

    @Lob
    private String publicKey;
}
