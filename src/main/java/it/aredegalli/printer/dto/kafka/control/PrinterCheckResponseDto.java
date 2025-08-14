package it.aredegalli.printer.dto.kafka.control;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrinterCheckResponseDto {

    private String statusCode;

    private String xPosition;
    private String yPosition;
    private String zPosition;
    private String ePosition;
    private String feed;

    private String layer;

    private String extruderStatus;
    private String extruderTemp;
    private String bedTemp;

    private String fanStatus;
    private String fanSpeed;

    private String commandOffset;
    private String lastCommand;

    private String averageSpeed;

    private String exceptions;

}
