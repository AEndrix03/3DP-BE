package it.aredegalli.printer.scheduled.kafka;

import it.aredegalli.printer.service.kafka.hearthbeat.PrinterHeartbeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class PrinterHeatbeatScheduler {

    private final PrinterHeartbeatService printerHeartbeatService;

    @Scheduled(fixedDelay = 60000)
    public void scheduleHeartbeatBroadcast() {
        //this.printerHeartbeatService.broadcastHeartbeatRequest();
    }

}
