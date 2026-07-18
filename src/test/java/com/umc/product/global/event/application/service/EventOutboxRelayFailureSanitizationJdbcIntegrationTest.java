package com.umc.product.global.event.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.event.adapter.out.EventPayloadDeserializer;
import com.umc.product.global.event.application.port.out.LoadEventOutboxPort;
import com.umc.product.global.event.application.port.out.SaveEventOutboxPort;
import com.umc.product.global.event.application.service.EventOutboxRelayJdbcTestFixtures.ExternalTestEvent;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.notification.domain.exception.EmailDomainException;
import com.umc.product.notification.domain.exception.EmailErrorCode;
import com.umc.product.support.IntegrationTestSupport;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.tracing.Tracer;

@DisplayName("EventOutboxRelayService JDBC failure sanitization")
@TestPropertySource(properties = "app.event-outbox.relay-enabled=false")
class EventOutboxRelayFailureSanitizationJdbcIntegrationTest extends IntegrationTestSupport {

    private static final String RAW_EMAIL = "recipient@example.com";
    private static final String RAW_SECRET = "prompt-injection-secret";

    @Autowired
    DataSource dataSource;

    @Autowired
    LoadEventOutboxPort loadEventOutboxPort;

    @Autowired
    SaveEventOutboxPort saveEventOutboxPort;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("listener 실패는 stable code 또는 class만 저장하고 raw PII를 남기지 않는다")
    void stores_stable_failure_codes_without_raw_pii() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        Instant dueAt = Instant.now().minusSeconds(1);
        EventOutbox businessFailure = EventOutboxRelayJdbcTestFixtures.externalOutbox(mapper, "business", dueAt);
        EventOutbox runtimeFailure = EventOutboxRelayJdbcTestFixtures.externalOutbox(mapper, "runtime", dueAt);
        saveEventOutboxPort.save(businessFailure);
        saveEventOutboxPort.save(runtimeFailure);
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

    private record FailureState(
        String status,
        int attempts,
        String lastError,
        Instant nextAttemptAt
    ) {
    }
}
