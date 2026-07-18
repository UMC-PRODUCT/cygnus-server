package com.umc.product.global.event.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.event.adapter.out.EventPayloadSerializer;
import com.umc.product.global.event.application.port.out.LoadEventOutboxPort;
import com.umc.product.global.event.application.port.out.SaveEventOutboxPort;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.EventOutboxStatus;
import com.umc.product.global.event.domain.OutboxDispatchMode;

final class EventOutboxRelayJdbcTestFixtures {

    private EventOutboxRelayJdbcTestFixtures() {
    }

    static EventOutbox externalOutbox(ObjectMapper mapper, String marker, Instant availableAt) {
        ExternalTestEvent event = ExternalTestEvent.create(marker);
        EventPayloadSerializer.SerializationResult serialized =
            new EventPayloadSerializer(mapper).serializeWithFingerprint(event);
        return EventOutbox.record(
            event,
            serialized.payload(),
            serialized.fingerprint(),
            availableAt
        );
    }

    record TransactionalTestEvent(
        UUID eventId,
        Instant occurredAt
    ) implements DomainEvent {

        static TransactionalTestEvent create() {
            return new TransactionalTestEvent(UUID.randomUUID(), Instant.now());
        }

        @Override
        public String eventType() {
            return "test.transactional.created";
        }
    }

    record ExternalTestEvent(
        UUID eventId,
        Instant occurredAt,
        String marker
    ) implements DomainEvent {

        static ExternalTestEvent create() {
            return create("single");
        }

        static ExternalTestEvent create(String marker) {
            return new ExternalTestEvent(UUID.randomUUID(), Instant.now(), marker);
        }

        @Override
        public String eventType() {
            return "test.external.created";
        }

        @Override
        public OutboxDispatchMode outboxDispatchMode() {
            return OutboxDispatchMode.NON_TRANSACTIONAL;
        }
    }

    static final class RecordingSaveEventOutboxPort implements SaveEventOutboxPort {

        final List<EventOutboxStatus> savedStatuses = new ArrayList<>();

        @Override
        public void save(EventOutbox eventOutbox) {
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

    static final class StaticLoadEventOutboxPort implements LoadEventOutboxPort {

        private final List<EventOutbox> outboxes;

        StaticLoadEventOutboxPort(List<EventOutbox> outboxes) {
            this.outboxes = outboxes;
        }

        @Override
        public List<EventOutbox> listPublishable(int limit, Instant now) {
            return outboxes.stream()
                .filter(outbox -> outbox.getStatus() == EventOutboxStatus.PENDING
                    || outbox.getStatus() == EventOutboxStatus.PROCESSING)
                .filter(outbox -> !outbox.getNextAttemptAt().isAfter(now))
                .limit(limit)
                .toList();
        }

        @Override
        public Optional<EventOutbox> findByEventId(UUID eventId) {
            return outboxes.stream()
                .filter(outbox -> outbox.getEventId().equals(eventId))
                .findFirst();
        }
    }
}
