package com.umc.product.global.event.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.EventOutbox;

@DisplayName("EventPayloadDeserializer")
class EventPayloadDeserializerTest {

    @Test
    @DisplayName("eventClass와 payload로 DomainEvent를 복원한다")
    void deserialize() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        EventPayloadDeserializer deserializer = new EventPayloadDeserializer(objectMapper);
        TestEvent event = TestEvent.create("test.created", "hello");
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event));

        DomainEvent result = deserializer.deserialize(outbox);

        assertThat(result).isInstanceOf(TestEvent.class);
        TestEvent restored = (TestEvent) result;
        assertThat(restored.eventId()).isEqualTo(event.eventId());
        assertThat(restored.occurredAt()).isEqualTo(event.occurredAt());
        assertThat(restored.eventType()).isEqualTo(event.eventType());
        assertThat(restored.message()).isEqualTo(event.message());
    }

    @Test
    @DisplayName("eventClass가 DomainEvent 타입이 아니면 예외를 던진다")
    void domain_event_타입_아님() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadDeserializer deserializer = new EventPayloadDeserializer(objectMapper);
        EventOutbox outbox = EventOutbox.record(TestEvent.create("test.created", "hello"), "{}");
        ReflectionTestUtils.setField(outbox, "eventClass", String.class.getName());

        assertThatThrownBy(() -> deserializer.deserialize(outbox))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("DomainEvent 타입이 아닙니다");
    }

    @Test
    @DisplayName("eventClass를 찾을 수 없으면 예외를 던진다")
    void event_class_없음() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadDeserializer deserializer = new EventPayloadDeserializer(objectMapper);
        EventOutbox outbox = EventOutbox.record(TestEvent.create("test.created", "hello"), "{}");
        ReflectionTestUtils.setField(outbox, "eventClass", "com.umc.product.NoSuchEvent");

        assertThatThrownBy(() -> deserializer.deserialize(outbox))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("event class를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("payload JSON을 복원할 수 없으면 예외를 던진다")
    void payload_복원_실패() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadDeserializer deserializer = new EventPayloadDeserializer(objectMapper);
        EventOutbox outbox = EventOutbox.record(TestEvent.create("test.created", "hello"), "{}");
        ReflectionTestUtils.setField(outbox, "payload", "{invalid");

        assertThatThrownBy(() -> deserializer.deserialize(outbox))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("event outbox payload 역직렬화에 실패했습니다");
    }

    private record TestEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        String message
    ) implements DomainEvent {

        static TestEvent create(String eventType, String message) {
            return new TestEvent(UUID.randomUUID(), Instant.now(), eventType, message);
        }
    }
}
