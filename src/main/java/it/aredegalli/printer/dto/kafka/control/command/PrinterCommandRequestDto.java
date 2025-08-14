package it.aredegalli.printer.dto.kafka.control.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrinterCommandRequestDto {

    private String driverId;
    private String requestId;

    private String command;
    private Integer priority = 0;

}
