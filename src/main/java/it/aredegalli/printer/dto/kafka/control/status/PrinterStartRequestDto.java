package it.aredegalli.printer.dto.kafka.control.status;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Blob;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrinterStartRequestDto {

    private String driverId;

    private String startGCode;
    private String endGCode;

    private Blob gcode;
}
