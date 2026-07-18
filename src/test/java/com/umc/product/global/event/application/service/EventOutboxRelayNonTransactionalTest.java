package com.umc.product.global.event.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.event.adapter.out.EventPayloadDeserializer;
import com.umc.product.global.event.adapter.out.EventPayloadSerializer;
import com.umc.product.global.event.application.service.EventOutboxRelayTestFixtures.FakeLoadEventOutboxPort;
import com.umc.product.global.event.application.service.EventOutboxRelayTestFixtures.FakeSaveEventOutboxPort;
import com.umc.product.global.event.application.service.EventOutboxRelayTestFixtures.LocalTransactionManager;
import com.umc.product.global.event.application.service.EventOutboxRelayTestFixtures.NonTransactionalTestEvent;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.EventOutboxStatus;

import io.micrometer.tracing.Tracer;

@DisplayName("EventOutboxRelayService non-transactional dispatch")
class EventOutboxRelayNonTransactionalTest {

    @Test
    @DisplayName("non-transactional 이벤트는 listener 실행 후 별도 트랜잭션으로 published 처리한다")
    void relay_non_transactional_dispatch() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        NonTransactionalTestEvent event = NonTransactionalTestEvent.create("test.external.created", "hello");
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event));
        FakeSaveEventOutboxPort savePort = new FakeSaveEventOutboxPort();
        AtomicBoolean transactionActiveDuringPublish = new AtomicBoolean(true);
        ApplicationEventPublisher publisher = ignored ->
            transactionActiveDuringPublish.set(TransactionSynchronizationManager.isActualTransactionActive());
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            new FakeLoadEventOutboxPort(List.of(outbox)),
            savePort,
            new EventPayloadDeserializer(objectMapper),
            publisher,
            new LocalTransactionManager(),
            Tracer.NOOP,
            100,
            3
        );

        relayService.relay();

        assertThat(transactionActiveDuringPublish).isFalse();
        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PUBLISHED);
        assertThat(savePort.savedStatuses).contains(EventOutboxStatus.PROCESSING, EventOutboxStatus.PUBLISHED);
    }

    @Test
    @DisplayName("non-transactional listener 예외는 attempts를 증가시키고 pending 재시도로 연결한다")
    void relay_non_transactional_listener_failure() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        NonTransactionalTestEvent event = NonTransactionalTestEvent.create("test.external.created", "hello");
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event));
        FakeSaveEventOutboxPort savePort = new FakeSaveEventOutboxPort();
        ApplicationEventPublisher publisher = ignored -> {
            throw new IllegalStateException("external call failed");
        };
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            new FakeLoadEventOutboxPort(List.of(outbox)),
            savePort,
            new EventPayloadDeserializer(objectMapper),
            publisher,
            new LocalTransactionManager(),
            Tracer.NOOP,
            100,
            3
        );

        relayService.relay();

        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PENDING);
        assertThat(outbox.getAttempts()).isEqualTo(1);
        assertThat(outbox.getLastError()).isEqualTo(IllegalStateException.class.getName());
        assertThat(savePort.savedStatuses).contains(EventOutboxStatus.PROCESSING, EventOutboxStatus.PENDING);
    }

    @Test
    @DisplayName("listener 실패 후 lease가 다시 due가 되면 다음 relay에서 재시도해 published 처리한다")
    void relay_retries_failed_listener_on_next_due_run() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        NonTransactionalTestEvent event = NonTransactionalTestEvent.create("test.external.created", "hello");
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event));
        AtomicInteger publishAttempts = new AtomicInteger();
        ApplicationEventPublisher publisher = ignored -> {
            if (publishAttempts.getAndIncrement() == 0) {
                throw new IllegalStateException("first listener interrupted");
            }
        };
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            new FakeLoadEventOutboxPort(List.of(outbox)),
            new FakeSaveEventOutboxPort(),
            new EventPayloadDeserializer(objectMapper),
            publisher,
            new LocalTransactionManager(),
            Tracer.NOOP,
            1,
            3
        );

        relayService.relay();
        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PENDING);
        assertThat(outbox.getAttempts()).isEqualTo(1);

        outbox.markProcessing(Instant.EPOCH);
        relayService.relay();

        assertThat(publishAttempts).hasValue(2);
        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PUBLISHED);
        assertThat(outbox.getAttempts()).isEqualTo(1);
    }
}
