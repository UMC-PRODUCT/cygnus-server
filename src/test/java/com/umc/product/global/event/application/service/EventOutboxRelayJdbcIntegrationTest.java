package com.umc.product.global.event.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.event.adapter.out.EventPayloadDeserializer;
import com.umc.product.global.event.adapter.out.EventPayloadSerializer;
import com.umc.product.global.event.application.port.out.SaveEventOutboxPort;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.EventOutboxStatus;
import com.umc.product.global.event.domain.OutboxDispatchMode;
import com.umc.product.support.IntegrationTestSupport;
import com.zaxxer.hikari.HikariDataSource;

import io.micrometer.tracing.Tracer;

@DisplayName("EventOutboxRelayService JDBC 트랜잭션")
class EventOutboxRelayJdbcIntegrationTest extends IntegrationTestSupport {

    @Autowired
    DataSource dataSource;

    @Test
    @DisplayName("non-transactional listener 실행 중에는 JDBC connection을 점유하지 않는다")
    void non_transactional_listener_releases_jdbc_connection() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(mapper);
        ExternalTestEvent event = ExternalTestEvent.create();
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event));
        RecordingSaveEventOutboxPort savePort = new RecordingSaveEventOutboxPort();
        HikariDataSource hikariDataSource = dataSource.unwrap(HikariDataSource.class);
        AtomicBoolean transactionActiveDuringDispatch = new AtomicBoolean(true);
        AtomicInteger activeConnectionsDuringDispatch = new AtomicInteger(-1);
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            (limit, now) -> List.of(outbox),
            savePort,
            new EventPayloadDeserializer(mapper),
            ignored -> {
                transactionActiveDuringDispatch.set(TransactionSynchronizationManager.isActualTransactionActive());
                activeConnectionsDuringDispatch.set(
                    hikariDataSource.getHikariPoolMXBean().getActiveConnections()
                );
            },
            new DataSourceTransactionManager(dataSource),
            Tracer.NOOP,
            100,
            3
        );

        relayService.relay();

        assertThat(transactionActiveDuringDispatch).isFalse();
        assertThat(activeConnectionsDuringDispatch).hasValue(0);
        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PUBLISHED);
        assertThat(savePort.savedStatuses).contains(EventOutboxStatus.PROCESSING, EventOutboxStatus.PUBLISHED);
    }

    private record ExternalTestEvent(
        UUID eventId,
        Instant occurredAt
    ) implements DomainEvent {

        static ExternalTestEvent create() {
            return new ExternalTestEvent(UUID.randomUUID(), Instant.now());
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

    private static class RecordingSaveEventOutboxPort implements SaveEventOutboxPort {

        private final List<EventOutboxStatus> savedStatuses = new ArrayList<>();

        @Override
        public void save(EventOutbox eventOutbox) {
            savedStatuses.add(eventOutbox.getStatus());
        }

        @Override
        public void saveAll(Collection<EventOutbox> eventOutboxes) {
            eventOutboxes.forEach(eventOutbox -> savedStatuses.add(eventOutbox.getStatus()));
        }
    }
}
