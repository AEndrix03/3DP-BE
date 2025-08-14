package it.aredegalli.printer.dto.kafka.control.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PrinterCommandResponseDto {

    private String driverId;
    private String requestId;

    private Boolean ok;

    private String exception;

    private String info;

}
