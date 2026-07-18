package com.umc.product.global.event.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.event.adapter.out.EventPayloadDeserializer;
import com.umc.product.global.event.application.port.out.LoadEventOutboxPort;
import com.umc.product.global.event.application.port.out.SaveEventOutboxPort;
import com.umc.product.global.event.application.service.EventOutboxRelayJdbcTestFixtures.ExternalTestEvent;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.support.IntegrationTestSupport;
import com.zaxxer.hikari.HikariDataSource;

import io.micrometer.tracing.Tracer;

@DisplayName("EventOutboxRelayService JDBC claim ordering")
@TestPropertySource(properties = "app.event-outbox.relay-enabled=false")
class EventOutboxRelayClaimJdbcIntegrationTest extends IntegrationTestSupport {

    @Autowired
    DataSource dataSource;

    @Autowired
    LoadEventOutboxPort loadEventOutboxPort;

    @Autowired
    SaveEventOutboxPort saveEventOutboxPort;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("첫 non-transactional listener가 끝나기 전에는 다음 outbox를 claim하지 않는다")
    void claims_next_outbox_only_after_non_transactional_listener_completes() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        Instant dueAt = Instant.now().minusSeconds(1);
        EventOutbox first = EventOutboxRelayJdbcTestFixtures.externalOutbox(mapper, "first", dueAt);
        EventOutbox second = EventOutboxRelayJdbcTestFixtures.externalOutbox(mapper, "second", dueAt);
        saveEventOutboxPort.save(first);
        saveEventOutboxPort.save(second);
        HikariDataSource hikariDataSource = dataSource.unwrap(HikariDataSource.class);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        CountDownLatch firstListenerStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstListener = new CountDownLatch(1);
        AtomicBoolean transactionActiveDuringDispatch = new AtomicBoolean(true);
        AtomicInteger activeConnectionsDuringDispatch = new AtomicInteger(-1);
        List<String> dispatchOrder = new ArrayList<>();
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            loadEventOutboxPort,
            saveEventOutboxPort,
            new EventPayloadDeserializer(mapper),
            event -> {
                ExternalTestEvent delivered = (ExternalTestEvent) event;
                dispatchOrder.add(delivered.marker());
                if (delivered.eventId().equals(first.getEventId())) {
                    transactionActiveDuringDispatch.set(
                        TransactionSynchronizationManager.isActualTransactionActive()
                    );
                    activeConnectionsDuringDispatch.set(
                        hikariDataSource.getHikariPoolMXBean().getActiveConnections()
                    );
                    firstListenerStarted.countDown();
                    await(releaseFirstListener);
                }
            },
            transactionManager,
            Tracer.NOOP,
            2,
            3
        );
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> relay = executor.submit(relayService::relay);

        try {
            assertThat(firstListenerStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(outboxStatus(jdbcTemplate, first.getEventId())).isEqualTo("PROCESSING");
            assertThat(outboxStatus(jdbcTemplate, second.getEventId())).isEqualTo("PENDING");
            assertThat(transactionActiveDuringDispatch).isFalse();
            assertThat(activeConnectionsDuringDispatch).hasValue(0);

            releaseFirstListener.countDown();
            relay.get(10, TimeUnit.SECONDS);

            assertThat(dispatchOrder).containsExactly("first", "second");
            assertThat(outboxStatus(jdbcTemplate, first.getEventId())).isEqualTo("PUBLISHED");
            assertThat(outboxStatus(jdbcTemplate, second.getEventId())).isEqualTo("PUBLISHED");
        } finally {
            releaseFirstListener.countDown();
            relay.cancel(true);
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private String outboxStatus(JdbcTemplate jdbcTemplate, UUID eventId) {
        return jdbcTemplate.queryForObject(
            "SELECT status FROM event_outbox WHERE event_id = ?",
            String.class,
            eventId
        );
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("outbox listener latch timed out");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("outbox listener latch interrupted", exception);
        }
    }
}
