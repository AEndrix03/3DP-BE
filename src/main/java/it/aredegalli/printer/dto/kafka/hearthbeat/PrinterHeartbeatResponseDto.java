package it.aredegalli.printer.dto.kafka.hearthbeat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrinterHeartbeatResponseDto {

    private String driverId;
    private String statusCode;

}