package com.umc.product.global.event.adapter.out.eventbridge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.event.application.port.out.PublishIntegrationEventPort;
import com.umc.product.global.event.domain.IntegrationEvent;
import com.umc.product.global.event.domain.IntegrationEventEnvelope;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.event-outbox.eventbridge",
    name = "enabled",
    havingValue = "true"
)
public class EventBridgeIntegrationEventAdapter implements PublishIntegrationEventPort {

    private final EventBridgeClient eventBridgeClient;
    private final EventBridgeIntegrationProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(IntegrationEvent event, String traceparent) {
        IntegrationEventEnvelope envelope = IntegrationEventEnvelope.from(event, traceparent);
        PutEventsResponse response = eventBridgeClient.putEvents(PutEventsRequest.builder()
            .entries(PutEventsRequestEntry.builder()
                .eventBusName(properties.eventBusName())
                .source(event.source())
                .detailType(event.eventType())
                .time(event.occurredAt())
                .detail(serialize(envelope))
                .build())
            .build());

        if (response.failedEntryCount() != null && response.failedEntryCount() > 0) {
            var failedEntry = response.entries().isEmpty() ? null : response.entries().getFirst();
            String errorCode = failedEntry == null ? "UNKNOWN" : failedEntry.errorCode();
            throw new IllegalStateException("EventBridge integration event 발행 실패: " + errorCode);
        }
    }

    private String serialize(IntegrationEventEnvelope envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Integration event envelope 직렬화에 실패했습니다.", exception);
        }
    }

}
