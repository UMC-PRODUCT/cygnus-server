package com.umc.product.global.event.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.event.adapter.out.EventPayloadDeserializer;
import com.umc.product.global.event.adapter.out.EventPayloadSerializer;
import com.umc.product.global.event.adapter.out.OutboxDomainEventPublisher;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.event.application.port.out.LoadEventOutboxPort;
import com.umc.product.global.event.application.port.out.SaveEventOutboxPort;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.global.event.domain.EventOutboxStatus;
import com.umc.product.global.event.domain.OutboxDispatchMode;
import com.umc.product.notification.domain.exception.EmailDomainException;
import com.umc.product.notification.domain.exception.EmailErrorCode;
import com.umc.product.support.IntegrationTestSupport;
import com.zaxxer.hikari.HikariDataSource;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.tracing.Tracer;

@DisplayName("EventOutboxRelayService JDBC 트랜잭션")
@TestPropertySource(properties = "app.event-outbox.relay-enabled=false")
class EventOutboxRelayJdbcIntegrationTest extends IntegrationTestSupport {

    private static final String RAW_EMAIL = "recipient@example.com";
    private static final String RAW_SECRET = "prompt-injection-secret";

    @Autowired
    DataSource dataSource;

    @Autowired
    List<DomainEventPublisher> domainEventPublishers;

    @Autowired
    LoadEventOutboxPort loadEventOutboxPort;

    @Autowired
    SaveEventOutboxPort saveEventOutboxPort;

    @Autowired
    PlatformTransactionManager transactionManager;

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

    @Test
    @DisplayName("첫 non-transactional listener가 끝나기 전에는 다음 outbox를 claim하지 않는다")
    void claims_next_outbox_only_after_non_transactional_listener_completes() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        Instant dueAt = Instant.now().minusSeconds(1);
        EventOutbox first = saveExternalEvent(mapper, "first", dueAt);
        EventOutbox second = saveExternalEvent(mapper, "second", dueAt);
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

    @Test
    @DisplayName("listener 실패는 stable code 또는 class만 저장하고 raw PII를 남기지 않는다")
    void stores_stable_failure_codes_without_raw_pii() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        Instant dueAt = Instant.now().minusSeconds(1);
        EventOutbox businessFailure = saveExternalEvent(mapper, "business", dueAt);
        EventOutbox runtimeFailure = saveExternalEvent(mapper, "runtime", dueAt);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Logger relayLogger = (Logger) LoggerFactory.getLogger(EventOutboxRelayService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        relayLogger.addAppender(appender);
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            loadEventOutboxPort,
            saveEventOutboxPort,
            new EventPayloadDeserializer(mapper),
            event -> {
                ExternalTestEvent delivered = (ExternalTestEvent) event;
                if ("business".equals(delivered.marker())) {
                    throw new EmailDomainException(
                        EmailErrorCode.EMAIL_SEND_FAILED,
                        new IllegalStateException(RAW_EMAIL + " " + RAW_SECRET)
                    );
                }
                throw new RuntimeException(RAW_EMAIL + " " + RAW_SECRET);
            },
            transactionManager,
            Tracer.NOOP,
            2,
            3
        );

        try {
            relayService.relay();
        } finally {
            relayLogger.detachAppender(appender);
            appender.stop();
        }

        FailureState businessState = failureState(jdbcTemplate, businessFailure.getEventId());
        FailureState runtimeState = failureState(jdbcTemplate, runtimeFailure.getEventId());
        assertThat(businessState.status()).isEqualTo("PENDING");
        assertThat(businessState.attempts()).isEqualTo(1);
        assertThat(businessState.lastError()).isEqualTo("EMAIL-0005");
        assertThat(runtimeState.status()).isEqualTo("PENDING");
        assertThat(runtimeState.attempts()).isEqualTo(1);
        assertThat(runtimeState.lastError()).isEqualTo(RuntimeException.class.getName());
        assertThat(businessState.nextAttemptAt()).isAfter(Instant.now());
        assertThat(runtimeState.nextAttemptAt()).isAfter(Instant.now());
        assertThat(rawFailureFragmentCount(jdbcTemplate)).isZero();
        assertThat(loggedText(appender.list)).doesNotContain(RAW_EMAIL, RAW_SECRET);
    }

    private record TransactionalTestEvent(
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

    private record ExternalTestEvent(
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

    private record FailureState(
        String status,
        int attempts,
        String lastError,
        Instant nextAttemptAt
    ) {
    }

    private static class RecordingSaveEventOutboxPort implements SaveEventOutboxPort {

        private final List<EventOutboxStatus> savedStatuses = new ArrayList<>();

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

    private static class StaticLoadEventOutboxPort implements LoadEventOutboxPort {

        private final List<EventOutbox> outboxes;

        private StaticLoadEventOutboxPort(List<EventOutbox> outboxes) {
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

    private EventOutbox saveExternalEvent(ObjectMapper mapper, String marker, Instant availableAt) {
        ExternalTestEvent event = ExternalTestEvent.create(marker);
        EventPayloadSerializer.SerializationResult serialized =
            new EventPayloadSerializer(mapper).serializeWithFingerprint(event);
        EventOutbox outbox = EventOutbox.record(
            event,
            serialized.payload(),
            serialized.fingerprint(),
            availableAt
        );
        saveEventOutboxPort.save(outbox);
        return outbox;
    }

    private String outboxStatus(JdbcTemplate jdbcTemplate, UUID eventId) {
        return jdbcTemplate.queryForObject(
            "SELECT status FROM event_outbox WHERE event_id = ?",
            String.class,
            eventId
        );
    }

    private FailureState failureState(JdbcTemplate jdbcTemplate, UUID eventId) {
        return jdbcTemplate.queryForObject(
            """
                SELECT status, attempts, last_error, next_attempt_at
                FROM event_outbox
                WHERE event_id = ?
                """,
            (resultSet, rowNumber) -> new FailureState(
                resultSet.getString("status"),
                resultSet.getInt("attempts"),
                resultSet.getString("last_error"),
                resultSet.getTimestamp("next_attempt_at").toInstant()
            ),
            eventId
        );
    }

    private long rawFailureFragmentCount(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM event_outbox
                WHERE POSITION(? IN COALESCE(last_error, '')) > 0
                   OR POSITION(? IN COALESCE(last_error, '')) > 0
                """,
            Long.class,
            RAW_EMAIL,
            RAW_SECRET
        );
    }

    private String loggedText(List<ILoggingEvent> events) {
        return events.stream()
            .map(event -> event.getFormattedMessage() + throwableText(event.getThrowableProxy()))
            .collect(Collectors.joining("\n"));
    }

    private String throwableText(IThrowableProxy throwable) {
        if (throwable == null) {
            return "";
        }
        return throwable.getClassName() + ":" + throwable.getMessage() + throwableText(throwable.getCause());
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("outbox listener latch timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("outbox listener latch interrupted", e);
        }
    }
}
