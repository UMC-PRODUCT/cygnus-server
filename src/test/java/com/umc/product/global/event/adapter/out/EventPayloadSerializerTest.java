package com.umc.product.global.event.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.EventOutbox;

@DisplayName("EventPayloadSerializer")
class EventPayloadSerializerTest {

    @Test
    @DisplayName("도메인 이벤트를 JSON payload로 직렬화한다")
    void testCase001() {
        EventPayloadSerializer serializer = new EventPayloadSerializer(new ObjectMapper().findAndRegisterModules());
        TestEvent event = TestEvent.create("test.created", "hello");

        String payload = serializer.serialize(event);

        assertThat(payload).contains("\"eventType\":\"test.created\"");
        assertThat(payload).contains("\"message\":\"hello\"");
    }

    @Test
    @DisplayName("직렬화 실패 시 eventType을 포함한 예외를 던진다")
    void testCase002() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        TestEvent event = TestEvent.create("test.created", "hello");
        given(objectMapper.writeValueAsString(event))
            .willThrow(new JsonMappingException(null, "boom"));

        assertThatThrownBy(() -> serializer.serialize(event))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("eventType=test.created");
    }

    @Test
    @DisplayName("canonical tree 생성 실패도 eventType을 포함한 예외로 매핑한다")
    void testCase003() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        TestEvent event = TestEvent.create("test.created", "hello");
        given(objectMapper.writeValueAsString(event)).willReturn("{}");
        given(objectMapper.readTree("{}")).willThrow(new JsonMappingException(null, "boom"));

        assertThatThrownBy(() -> serializer.serializeWithFingerprint(event))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("eventType=test.created");
    }

    @Test
    @DisplayName("null 이벤트의 기존 payload와 typed 결과를 유지한다")
    void testCase004() {
        EventPayloadSerializer serializer = new EventPayloadSerializer(new ObjectMapper().findAndRegisterModules());

        EventPayloadSerializer.SerializationResult result = serializer.serializeWithFingerprint(null);

        assertThat(serializer.serialize(null)).isEqualTo("null");
        assertThat(result.payload()).isEqualTo("null");
        assertThat(result.fingerprint()).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("메타데이터와 Map 삽입 순서가 달라도 동일한 fingerprint를 반환한다")
    void testCase005() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        Instant firstOccurredAt = Instant.parse("2026-07-18T00:00:00Z");
        Instant secondOccurredAt = Instant.parse("2026-07-19T00:00:00Z");

        Map<String, Object> firstDetails = new LinkedHashMap<>();
        firstDetails.put("zeta", "same");
        firstDetails.put("nested", nestedDetails("nested-event"));
        firstDetails.put("array", List.of("first", "second"));
        firstDetails.put("nullable", null);
        firstDetails.put("unicode", "<tag>눈");

        Map<String, Object> secondDetails = new LinkedHashMap<>();
        secondDetails.put("unicode", "<tag>눈");
        secondDetails.put("nullable", null);
        secondDetails.put("array", List.of("first", "second"));
        secondDetails.put("nested", nestedDetailsReversed("nested-event"));
        secondDetails.put("zeta", "same");

        EventPayloadSerializer.SerializationResult first = serializer.serializeWithFingerprint(
            new RichTestEvent(UUID.randomUUID(), firstOccurredAt, "test.created", firstDetails)
        );
        EventPayloadSerializer.SerializationResult second = serializer.serializeWithFingerprint(
            new RichTestEvent(UUID.randomUUID(), secondOccurredAt, "test.created", secondDetails)
        );

        assertThat(first.fingerprint()).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(second.fingerprint()).isEqualTo(first.fingerprint());
        assertThat(first.payload()).contains("\"eventType\":\"test.created\"");
        assertThat(first.payload()).contains("\"eventType\":\"nested-event\"");
    }

    @Test
    @DisplayName("typed serialization 결과의 full payload를 기존 deserializer로 복원한다")
    void testCase006() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        EventPayloadDeserializer deserializer = new EventPayloadDeserializer(objectMapper);
        RichTestEvent event = new RichTestEvent(
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            Instant.parse("2026-07-18T00:00:00Z"),
            "test.created",
            detailsWith("nested-event", List.of("first", "second"), null, 1)
        );

        EventPayloadSerializer.SerializationResult result = serializer.serializeWithFingerprint(event);
        RichTestEvent restored = (RichTestEvent) deserializer.deserialize(EventOutbox.record(event, result.payload()));

        assertThat(restored).isEqualTo(event);
    }

    @Test
    @DisplayName("nested metadata key의 값이 달라지면 fingerprint가 달라진다")
    void testCase007() {
        EventPayloadSerializer serializer = new EventPayloadSerializer(new ObjectMapper().findAndRegisterModules());

        String first = fingerprint(serializer, detailsWith("nested-event-a", List.of("first", "second"), null, 1));
        String second = fingerprint(serializer, detailsWith("nested-event-b", List.of("first", "second"), null, 1));

        assertThat(second).isNotEqualTo(first);
    }

    @Test
    @DisplayName("nested array 순서가 달라지면 fingerprint가 달라진다")
    void testCase008() {
        EventPayloadSerializer serializer = new EventPayloadSerializer(new ObjectMapper().findAndRegisterModules());

        String first = fingerprint(serializer, detailsWith("nested-event", List.of("first", "second"), null, 1));
        String second = fingerprint(serializer, detailsWith("nested-event", List.of("second", "first"), null, 1));

        assertThat(second).isNotEqualTo(first);
    }

    @Test
    @DisplayName("null 값이 달라지면 fingerprint가 달라진다")
    void testCase009() {
        EventPayloadSerializer serializer = new EventPayloadSerializer(new ObjectMapper().findAndRegisterModules());

        String first = fingerprint(serializer, detailsWith("nested-event", List.of("first", "second"), null, 1));
        String second = fingerprint(serializer, detailsWith("nested-event", List.of("first", "second"), "present", 1));

        assertThat(second).isNotEqualTo(first);
    }

    @Test
    @DisplayName("정수와 소수 표현이 달라지면 fingerprint가 달라진다")
    void testCase010() {
        EventPayloadSerializer serializer = new EventPayloadSerializer(new ObjectMapper().findAndRegisterModules());

        String first = fingerprint(serializer, detailsWith("nested-event", List.of("first", "second"), null, 1));
        String second = fingerprint(serializer, detailsWith("nested-event", List.of("first", "second"), null, 1.0));

        assertThat(second).isNotEqualTo(first);
    }

    @Test
    @DisplayName("Unicode와 HTML-like 문자열 값이 달라지면 fingerprint가 달라진다")
    void testCase011() {
        EventPayloadSerializer serializer = new EventPayloadSerializer(new ObjectMapper().findAndRegisterModules());
        Map<String, Object> firstDetails = detailsWith("nested-event", List.of("first", "second"), null, 1);
        Map<String, Object> secondDetails = detailsWith("nested-event", List.of("first", "second"), null, 1);
        firstDetails.put("untrusted", "<script>눈");
        secondDetails.put("untrusted", "<script>☃");

        String first = fingerprint(serializer, firstDetails);
        String second = fingerprint(serializer, secondDetails);

        assertThat(second).isNotEqualTo(first);
    }

    private String fingerprint(EventPayloadSerializer serializer, Map<String, Object> details) {
        return serializer.serializeWithFingerprint(
            new RichTestEvent(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                Instant.parse("2026-07-18T00:00:00Z"),
                "test.created",
                details
            )
        ).fingerprint();
    }

    private Map<String, Object> detailsWith(
        String nestedEventType,
        List<String> array,
        Object nullable,
        Number number
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("nested", nestedDetails(nestedEventType));
        details.put("array", array);
        details.put("nullable", nullable);
        details.put("number", number);
        return details;
    }

    private Map<String, Object> nestedDetails(String nestedEventType) {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("eventType", nestedEventType);
        nested.put("eventId", "nested-id");
        nested.put("occurredAt", "nested-time");
        return nested;
    }

    private Map<String, Object> nestedDetailsReversed(String nestedEventType) {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("occurredAt", "nested-time");
        nested.put("eventId", "nested-id");
        nested.put("eventType", nestedEventType);
        return nested;
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

    private record RichTestEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        Map<String, Object> details
    ) implements DomainEvent {
    }
}
