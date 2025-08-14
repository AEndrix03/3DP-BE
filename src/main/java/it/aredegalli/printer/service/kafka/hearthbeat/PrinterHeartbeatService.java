package it.aredegalli.printer.service.kafka.hearthbeat;

import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

public interface PrinterHeartbeatService {
    CompletableFuture<SendResult<String, Object>> broadcastHeartbeatRequest();
}
