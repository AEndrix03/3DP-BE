package it.aredegalli.printer.model.printer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "printer_status")
public class PrinterStatus {
    @Id
    @Column(length = 100)
    private String code;

    private String description;
}
