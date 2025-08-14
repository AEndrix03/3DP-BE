package it.aredegalli.printer.dto.kafka.control.status;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrinterStopRequestDto {

    private String driverId;


}
