package com.umc.product.global.event.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.event.adapter.out.EventPayloadDeserializer;
import com.umc.product.global.event.adapter.out.EventPayloadSerializer;
import com.umc.product.global.event.application.service.EventOutboxRelayTestFixtures.CapturingApplicationEventPublisher;
import com.umc.product.global.event.application.service.EventOutboxRelayTestFixtures.FakeLoadEventOutboxPort;
import com.umc.product.global.event.application.service.EventOutboxRelayTestFixtures.FakeSaveEventOutboxPort;
import com.umc.product.global.event.application.service.EventOutboxRelayTestFixtures.LocalTransactionManager;
import com.umc.product.global.event.application.service.EventOutboxRelayTestFixtures.TestEvent;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.EventOutboxStatus;

import io.micrometer.tracing.test.simple.SimpleSpan;
import io.micrometer.tracing.test.simple.SimpleTracer;

@DisplayName("EventOutboxRelayService tracing")
class EventOutboxRelayTracingTest {

    @Test
    @DisplayName("저장된 traceparent가 있으면 relay 처리 span에 원 요청 trace로의 span link를 부착한다")
    void testCase001() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        TestEvent event = TestEvent.create("test.created", "hello");
        String traceId = "0af7651916cd43dd8448eb211c80319c";
        String spanId = "b7ad6b7169203331";
        String traceparent = "00-" + traceId + "-" + spanId + "-01";
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event), traceparent);
        FakeLoadEventOutboxPort loadPort = new FakeLoadEventOutboxPort(List.of(outbox));
        FakeSaveEventOutboxPort savePort = new FakeSaveEventOutboxPort();
        SimpleTracer tracer = new SimpleTracer();
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            loadPort,
            savePort,
            new EventPayloadDeserializer(objectMapper),
            new CapturingApplicationEventPublisher(),
            new LocalTransactionManager(),
            tracer,
            100,
            3
        );

        relayService.relay();

        SimpleSpan relaySpan = tracer.getSpans().stream()
            .filter(span -> "outbox.relay.publish".equals(span.getName()))
            .findFirst()
            .orElseThrow();
        assertThat(relaySpan.getLinks())
            .extracting(link -> link.getTraceContext().traceId())
            .containsExactly(traceId);
        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PUBLISHED);
    }

    @Test
    @DisplayName("traceparent가 없으면 link 없이 relay 처리 span만 생성하고 정상 발행한다")
    void testCase002() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        TestEvent event = TestEvent.create("test.created", "hello");
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event));
        FakeLoadEventOutboxPort loadPort = new FakeLoadEventOutboxPort(List.of(outbox));
        FakeSaveEventOutboxPort savePort = new FakeSaveEventOutboxPort();
        SimpleTracer tracer = new SimpleTracer();
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            loadPort,
            savePort,
            new EventPayloadDeserializer(objectMapper),
            new CapturingApplicationEventPublisher(),
            new LocalTransactionManager(),
            tracer,
            100,
            3
        );

        relayService.relay();

        SimpleSpan relaySpan = tracer.getSpans().stream()
            .filter(span -> "outbox.relay.publish".equals(span.getName()))
            .findFirst()
            .orElseThrow();
        assertThat(relaySpan.getLinks()).isEmpty();
        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PUBLISHED);
    }
}
