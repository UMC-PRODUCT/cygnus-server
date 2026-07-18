package com.umc.product.global.event.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import com.umc.product.global.event.application.port.out.LoadEventOutboxPort;
import com.umc.product.global.event.application.port.out.SaveEventOutboxPort;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.EventOutboxStatus;
import com.umc.product.global.event.domain.OutboxDispatchMode;

final class EventOutboxRelayTestFixtures {

    private EventOutboxRelayTestFixtures() {
    }

    static final class FakeLoadEventOutboxPort implements LoadEventOutboxPort {

        private final List<EventOutbox> outboxes;
        final List<Integer> requestedLimits = new ArrayList<>();

        FakeLoadEventOutboxPort(List<EventOutbox> outboxes) {
            this.outboxes = outboxes;
        }

        @Override
        public List<EventOutbox> listPublishable(int limit, Instant now) {
            requestedLimits.add(limit);
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

    static final class FakeSaveEventOutboxPort implements SaveEventOutboxPort {

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

    static final class CapturingApplicationEventPublisher implements ApplicationEventPublisher {

        final List<Object> events = new ArrayList<>();

        @Override
        public void publishEvent(Object event) {
            events.add(event);
        }
    }

    record TestEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        String message
    ) implements DomainEvent {

        static TestEvent create(String eventType, String message) {
            return new TestEvent(UUID.randomUUID(), Instant.now(), eventType, message);
        }
    }

    record NonTransactionalTestEvent(
        UUID eventId,
        Instant occurredAt,
        String eventType,
        String message
    ) implements DomainEvent {

        static NonTransactionalTestEvent create(String eventType, String message) {
            return new NonTransactionalTestEvent(UUID.randomUUID(), Instant.now(), eventType, message);
        }

        @Override
        public OutboxDispatchMode outboxDispatchMode() {
            return OutboxDispatchMode.NON_TRANSACTIONAL;
        }
    }

    static final class LocalTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() throws TransactionException {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) throws TransactionException {
            return false;
        }
    }
}
