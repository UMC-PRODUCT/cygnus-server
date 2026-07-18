package com.umc.product.global.event.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.event.adapter.out.EventPayloadDeserializer;
import com.umc.product.global.event.adapter.out.EventPayloadSerializer;
import com.umc.product.global.event.application.port.out.SaveEventOutboxPort;
import com.umc.product.global.event.application.service.EventOutboxRelayTestFixtures.CapturingApplicationEventPublisher;
import com.umc.product.global.event.application.service.EventOutboxRelayTestFixtures.FakeLoadEventOutboxPort;
import com.umc.product.global.event.application.service.EventOutboxRelayTestFixtures.LocalTransactionManager;
import com.umc.product.global.event.application.service.EventOutboxRelayTestFixtures.NonTransactionalTestEvent;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.EventOutboxStatus;

import io.micrometer.tracing.Tracer;

@DisplayName("EventOutboxRelayService lease fencing")
class EventOutboxRelayLeaseFencingTest {

    @Test
    @DisplayName("non-transactional listener 성공 후 published 저장이 실패하면 재시도 대상(PENDING)으로 남긴다")
    void relay_published_저장_실패_재시도() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        NonTransactionalTestEvent event = NonTransactionalTestEvent.create("test.external.created", "hello");
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event));
        FakeLoadEventOutboxPort loadPort = new FakeLoadEventOutboxPort(List.of(outbox));
        FailOnPublishedSaveEventOutboxPort savePort = new FailOnPublishedSaveEventOutboxPort();
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

        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PENDING);
        assertThat(outbox.getAttempts()).isEqualTo(1);
        assertThat(publisher.events).hasSize(1);
        assertThat(savePort.savedStatuses).contains(EventOutboxStatus.PROCESSING, EventOutboxStatus.PENDING);
    }

    @Test
    @DisplayName("lease 소유권을 잃은 worker는 published 상태 저장 실패를 재시도로 덮어쓰지 않는다")
    void relay_optimistic_lock_failure_does_not_overwrite_new_owner() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        NonTransactionalTestEvent event = NonTransactionalTestEvent.create("test.external.created", "hello");
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event));
        LoseLeaseOnPublishedSaveEventOutboxPort savePort = new LoseLeaseOnPublishedSaveEventOutboxPort();
        CapturingApplicationEventPublisher publisher = new CapturingApplicationEventPublisher();
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

        assertThat(publisher.events).hasSize(1);
        assertThat(savePort.savedStatuses).containsExactly(EventOutboxStatus.PROCESSING);
    }

    private static class FailOnPublishedSaveEventOutboxPort implements SaveEventOutboxPort {

        private final List<EventOutboxStatus> savedStatuses = new ArrayList<>();

        @Override
        public void save(EventOutbox eventOutbox) {
            // published 상태 저장(= markPublished 영속화)만 실패시켜, 발행 단위 트랜잭션 실패를 재현한다.
            if (eventOutbox.getStatus() == EventOutboxStatus.PUBLISHED) {
                throw new IllegalStateException("published 저장 실패");
            }
            savedStatuses.add(eventOutbox.getStatus());
        }

        @Override
        public boolean saveIfAbsent(EventOutbox eventOutbox) {
            save(eventOutbox);
            return true;
        }

        @Override
        public void saveAll(Collection<EventOutbox> eventOutboxes) {
            eventOutboxes.forEach(eventOutbox -> savedStatuses.add(eventOutbox.getStatus()));
        }
    }

    private static class LoseLeaseOnPublishedSaveEventOutboxPort implements SaveEventOutboxPort {

        private final List<EventOutboxStatus> savedStatuses = new ArrayList<>();

        @Override
        public void save(EventOutbox eventOutbox) {
            if (eventOutbox.getStatus() == EventOutboxStatus.PUBLISHED) {
                throw new OptimisticLockingFailureException("outbox lease 소유권 변경");
            }
            savedStatuses.add(eventOutbox.getStatus());
        }

        @Override
        public boolean saveIfAbsent(EventOutbox eventOutbox) {
            save(eventOutbox);
            return true;
        }

        @Override
        public void saveAll(Collection<EventOutbox> eventOutboxes) {
            eventOutboxes.forEach(eventOutbox -> savedStatuses.add(eventOutbox.getStatus()));
        }
    }
}
