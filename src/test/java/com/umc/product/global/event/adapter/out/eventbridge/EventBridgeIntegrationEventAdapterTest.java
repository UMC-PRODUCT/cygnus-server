package com.umc.product.global.event.adapter.out.eventbridge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.event.domain.IntegrationEvent;

import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResultEntry;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventBridgeIntegrationEventAdapter")
class EventBridgeIntegrationEventAdapterTest {

    @Mock
    EventBridgeClient eventBridgeClient;

    @Test
    @DisplayName("공통 envelope을 custom bus로 보내고 EventBridge 수락을 성공으로 반환한다")
    void EventBridge_ACK_확인() {
        EventBridgeIntegrationEventAdapter adapter = adapter();
        given(eventBridgeClient.putEvents(org.mockito.ArgumentMatchers.any(PutEventsRequest.class)))
            .willReturn(PutEventsResponse.builder().failedEntryCount(0).build());
        TestIntegrationEvent event = TestIntegrationEvent.create();

        adapter.publish(event, "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01");

        ArgumentCaptor<PutEventsRequest> captor = ArgumentCaptor.forClass(PutEventsRequest.class);
        then(eventBridgeClient).should().putEvents(captor.capture());
        var entry = captor.getValue().entries().getFirst();
        assertThat(entry.eventBusName()).isEqualTo("notification-bus");
        assertThat(entry.detailType()).isEqualTo("test.integration.created.v1");
        assertThat(entry.source()).isEqualTo("umc-product.test");
        assertThat(entry.detail())
            .contains("\"schemaVersion\":1")
            .contains("\"eventId\":\"" + event.eventId() + "\"")
            .contains("\"traceparent\":\"00-0af7651916cd43dd8448eb211c80319c");
    }

    @Test
    @DisplayName("EventBridge가 entry를 거부하면 outbox 재시도로 연결할 예외를 던진다")
    void EventBridge_NACK_예외() {
        EventBridgeIntegrationEventAdapter adapter = adapter();
        given(eventBridgeClient.putEvents(org.mockito.ArgumentMatchers.any(PutEventsRequest.class)))
            .willReturn(PutEventsResponse.builder()
                .failedEntryCount(1)
                .entries(PutEventsResultEntry.builder().errorCode("InternalFailure").build())
                .build());

        assertThatThrownBy(() -> adapter.publish(TestIntegrationEvent.create(), null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("InternalFailure");
    }

    private EventBridgeIntegrationEventAdapter adapter() {
        return new EventBridgeIntegrationEventAdapter(
            eventBridgeClient,
            new EventBridgeIntegrationProperties(true, "ap-northeast-2", "notification-bus"),
            new ObjectMapper().findAndRegisterModules()
        );
    }

    private record TestIntegrationEvent(
        UUID eventId,
        Instant occurredAt,
        UUID requestId,
        Map<String, String> detail
    ) implements IntegrationEvent {

        static TestIntegrationEvent create() {
            return new TestIntegrationEvent(
                UUID.randomUUID(),
                Instant.parse("2026-07-23T00:00:00Z"),
                UUID.randomUUID(),
                Map.of("message", "hello")
            );
        }

        @Override
        public String eventType() {
            return "test.integration.created.v1";
        }

        @Override
        public String source() {
            return "umc-product.test";
        }
    }
}
