package it.aredegalli.printer.enums.kafka;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum KafkaTopicEnum {

    PRINTER_HEARTBEATH_REQUEST("printer-heartbeat-request"),
    PRINTER_CHECK_REQUEST("printer-check-request"),
    PRINTER_COMMAND_REQUEST("printer-command-request"),
    PRINTER_START_REQUEST("printer-start-request"),
    PRINTER_STOP_REQUEST("printer-stop-request"),
    PRINTER_PAUSE_REQUEST("printer-pause-request");

    private final String topicName;

}
