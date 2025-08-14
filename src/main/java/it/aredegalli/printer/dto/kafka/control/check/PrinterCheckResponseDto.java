package it.aredegalli.printer.dto.kafka.control.check;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrinterCheckResponseDto {

    private String jobId;
    private String driverId;

    private String jobStatusCode;
    private String printerStatusCode;

    private String xPosition;
    private String yPosition;
    private String zPosition;
    private String ePosition;
    private String feed;

    private String layer;
    private String layerHeight;

    private String extruderStatus;
    private String extruderTemp;
    private String bedTemp;

    private String fanStatus;
    private String fanSpeed;

    private String commandOffset;
    private String lastCommand;

    private String averageSpeed;

    private String exceptions;
    private String logs;

}
