package it.aredegalli.printer.dto.printer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class PrinterDto {

    private UUID id;
    private String name;
    private UUID driverId;
    private Instant lastSeen;
    private String image;

}
