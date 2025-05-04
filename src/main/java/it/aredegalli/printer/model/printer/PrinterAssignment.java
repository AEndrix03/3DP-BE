package it.aredegalli.printer.model.printer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "printer_assignment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_ref", "printer_id"}))
public class PrinterAssignment {
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_ref", nullable = false)
    private String userRef;

    @ManyToOne
    @JoinColumn(name = "printer_id")
    private Printer printer;

    private String permission;
}
