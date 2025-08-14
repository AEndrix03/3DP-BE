package it.aredegalli.printer.service.kafka;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum KafkaTopicEnum {

    PRINTER_HEARTBEATH_REQUEST("printer-heartbeat-request");

    private final String topicName;

}
