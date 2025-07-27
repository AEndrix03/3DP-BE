package it.aredegalli.printer.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Client subscription request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequestDto {
    private String userId;
    private String subscriptionType;
    private UUID specificQueueId;
}