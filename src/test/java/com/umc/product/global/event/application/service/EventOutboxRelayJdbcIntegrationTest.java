package com.umc.product.global.event.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.event.adapter.out.EventPayloadDeserializer;
import com.umc.product.global.event.adapter.out.EventPayloadSerializer;
import com.umc.product.global.event.adapter.out.OutboxDomainEventPublisher;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.event.application.service.EventOutboxRelayJdbcTestFixtures.ExternalTestEvent;
import com.umc.product.global.event.application.service.EventOutboxRelayJdbcTestFixtures.RecordingSaveEventOutboxPort;
import com.umc.product.global.event.application.service.EventOutboxRelayJdbcTestFixtures.StaticLoadEventOutboxPort;
import com.umc.product.global.event.application.service.EventOutboxRelayJdbcTestFixtures.TransactionalTestEvent;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.EventOutboxStatus;
import com.umc.product.support.IntegrationTestSupport;
import com.zaxxer.hikari.HikariDataSource;

import io.micrometer.tracing.Tracer;

@DisplayName("EventOutboxRelayService JDBC 트랜잭션")
@TestPropertySource(properties = "app.event-outbox.relay-enabled=false")
class EventOutboxRelayJdbcIntegrationTest extends IntegrationTestSupport {

    @Autowired
    DataSource dataSource;

    @Autowired
    List<DomainEventPublisher> domainEventPublishers;

    @Test
    @DisplayName("전체 application context는 outbox publisher 하나만 등록한다")
    void application_context_uses_single_outbox_publisher() {
        assertThat(domainEventPublishers)
            .singleElement()
            .isInstanceOf(OutboxDomainEventPublisher.class);
    }

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
            new StaticLoadEventOutboxPort(List.of(outbox)),
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

    @Test
    @DisplayName("transactional listener 실행 중에는 JDBC connection을 유지한다")
    void transactional_listener_keeps_jdbc_connection() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(mapper);
        TransactionalTestEvent event = TransactionalTestEvent.create();
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event));
        RecordingSaveEventOutboxPort savePort = new RecordingSaveEventOutboxPort();
        HikariDataSource hikariDataSource = dataSource.unwrap(HikariDataSource.class);
        AtomicBoolean transactionActiveDuringDispatch = new AtomicBoolean(false);
        AtomicInteger activeConnectionsDuringDispatch = new AtomicInteger(0);
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            new StaticLoadEventOutboxPort(List.of(outbox)),
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

        assertThat(transactionActiveDuringDispatch).isTrue();
        assertThat(activeConnectionsDuringDispatch).hasPositiveValue();
        assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PUBLISHED);
        assertThat(savePort.savedStatuses).contains(EventOutboxStatus.PROCESSING, EventOutboxStatus.PUBLISHED);
    }
}
