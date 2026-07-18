package com.umc.product.global.event.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.event.adapter.out.EventPayloadDeserializer;
import com.umc.product.global.event.adapter.out.EventPayloadSerializer;
import com.umc.product.global.event.application.service.EventOutboxRelayTestFixtures.CapturingApplicationEventPublisher;
import com.umc.product.global.event.application.service.EventOutboxRelayTestFixtures.FakeLoadEventOutboxPort;
import com.umc.product.global.event.application.service.EventOutboxRelayTestFixtures.FakeSaveEventOutboxPort;
import com.umc.product.global.event.application.service.EventOutboxRelayTestFixtures.LocalTransactionManager;
import com.umc.product.global.event.application.service.EventOutboxRelayTestFixtures.NonTransactionalTestEvent;
import com.umc.product.global.event.application.service.EventOutboxRelayTestFixtures.TestEvent;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.EventOutboxStatus;

import io.micrometer.tracing.Tracer;

@DisplayName("EventOutboxRelayService")
class EventOutboxRelayServiceTest {

    @Test
    @DisplayName("publishable outbox를 DomainEvent로 복원해 Spring event bus로 발행하고 published 처리한다")
    void relay_성공() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        TestEvent event = TestEvent.create("test.created", "hello");
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event));
        FakeLoadEventOutboxPort loadPort = new FakeLoadEventOutboxPort(List.of(outbox));
        FakeSaveEventOutboxPort savePort = new FakeSaveEventOutboxPort();
        CapturingApplicationEventPublisher publisher = new CapturingApplicationEventPublisher();
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            loadPort,
            savePort,
            new EventPayloadDeserializer(objectMapper),
            publisher,
            new LocalTransactionManager(),
            Tracer.NOOP,
            100,
            3
        );

        relayService.relay();

        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PUBLISHED);
        assertThat(publisher.events).hasSize(1);
        assertThat(publisher.events.getFirst()).isInstanceOf(TestEvent.class);
        assertThat(((TestEvent) publisher.events.getFirst()).message()).isEqualTo("hello");
        assertThat(savePort.savedStatuses).contains(EventOutboxStatus.PROCESSING, EventOutboxStatus.PUBLISHED);
    }

    @Test
    @DisplayName("각 listener 완료 후 다음 outbox 한 건만 claim한다")
    void relay_claims_one_row_immediately_before_dispatch() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        NonTransactionalTestEvent firstEvent = NonTransactionalTestEvent.create("test.external.created", "first");
        NonTransactionalTestEvent secondEvent = NonTransactionalTestEvent.create("test.external.created", "second");
        EventOutbox first = EventOutbox.record(firstEvent, serializer.serialize(firstEvent));
        EventOutbox second = EventOutbox.record(secondEvent, serializer.serialize(secondEvent));
        FakeLoadEventOutboxPort loadPort = new FakeLoadEventOutboxPort(List.of(first, second));
        List<List<EventOutboxStatus>> statusAtDispatch = new ArrayList<>();
        ApplicationEventPublisher publisher = ignored ->
            statusAtDispatch.add(List.of(first.getStatus(), second.getStatus()));
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            loadPort,
            new FakeSaveEventOutboxPort(),
            new EventPayloadDeserializer(objectMapper),
            publisher,
            new LocalTransactionManager(),
            Tracer.NOOP,
            2,
            3
        );

        relayService.relay();

        assertThat(loadPort.requestedLimits).containsExactly(1, 1);
        assertThat(statusAtDispatch).containsExactly(
            List.of(EventOutboxStatus.PROCESSING, EventOutboxStatus.PENDING),
            List.of(EventOutboxStatus.PUBLISHED, EventOutboxStatus.PROCESSING)
        );
        assertThat(first.getStatus()).isEqualTo(EventOutboxStatus.PUBLISHED);
        assertThat(second.getStatus()).isEqualTo(EventOutboxStatus.PUBLISHED);
    }

    @Test
    @DisplayName("이벤트 복원 또는 발행 실패 시 별도 상태 저장 트랜잭션에서 attempts를 증가시키고 pending으로 남긴다")
    void relay_실패_재시도() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        TestEvent event = TestEvent.create("test.created", "hello");
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event));
        FakeLoadEventOutboxPort loadPort = new FakeLoadEventOutboxPort(List.of(outbox));
        FakeSaveEventOutboxPort savePort = new FakeSaveEventOutboxPort();
        ApplicationEventPublisher publisher = ignored -> {
            throw new IllegalStateException("publish failed");
        };
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            loadPort,
            savePort,
            new EventPayloadDeserializer(objectMapper),
            publisher,
            new LocalTransactionManager(),
            Tracer.NOOP,
            100,
            3
        );

        Instant beforeRelay = Instant.now();
        relayService.relay();
        Instant afterRelay = Instant.now();

        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PENDING);
        assertThat(outbox.getAttempts()).isEqualTo(1);
        assertThat(outbox.getLastError()).isEqualTo(IllegalStateException.class.getName());
        assertThat(outbox.getNextAttemptAt())
            .isBetween(beforeRelay.plusSeconds(5), afterRelay.plusSeconds(5));
        assertThat(savePort.savedStatuses).contains(EventOutboxStatus.PROCESSING, EventOutboxStatus.PENDING);
    }

    @Test
    @DisplayName("최대 재시도 횟수에 도달하면 failed 상태로 저장한다")
    void relay_최대_재시도_도달() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        TestEvent event = TestEvent.create("test.created", "hello");
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event));
        outbox.recordFailure("previous", Instant.now(), 2);
        FakeLoadEventOutboxPort loadPort = new FakeLoadEventOutboxPort(List.of(outbox));
        FakeSaveEventOutboxPort savePort = new FakeSaveEventOutboxPort();
        ApplicationEventPublisher publisher = ignored -> {
            throw new IllegalStateException("publish failed");
        };
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            loadPort,
            savePort,
            new EventPayloadDeserializer(objectMapper),
            publisher,
            new LocalTransactionManager(),
            Tracer.NOOP,
            100,
            2
        );

        relayService.relay();

        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.FAILED);
        assertThat(outbox.getAttempts()).isEqualTo(2);
        assertThat(savePort.savedStatuses).contains(EventOutboxStatus.PROCESSING, EventOutboxStatus.FAILED);
    }
}
