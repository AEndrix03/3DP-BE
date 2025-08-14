package it.aredegalli.printer.dto.kafka.control.check;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrinterCheckRequestDto {

    private String driverId;
    private String jobId;

    private String criteria; //Aggiorna il criterio di richiesta check: es. ogni n comandi, ogni m secondi, etc...

}
