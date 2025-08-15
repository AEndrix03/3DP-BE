package it.aredegalli.printer.dto.kafka.control.status;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrinterStartRequestDto {

    private String driverId;

    private String startGCode;
    private String endGCode;

    private String gcodeUrl;
}
