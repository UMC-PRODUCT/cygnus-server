package com.umc.product.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.umc.product.global.event.adapter.in.scheduler.EventOutboxPoller;
import com.umc.product.global.event.adapter.out.persistence.EventOutboxJpaRepository;
import com.umc.product.global.event.application.service.EventOutboxRelayService;
import com.umc.product.global.event.domain.EventOutboxStatus;
import com.umc.product.notification.application.port.in.SendEmailUseCase;
import com.umc.product.notification.application.port.in.dto.SendTemplateEmailCommand;
import com.umc.product.notification.application.port.in.dto.TemplateEmailRequestInfo;
import com.umc.product.notification.application.port.out.SendEmailPort;
import com.umc.product.notification.application.port.out.dto.EmailMessage;
import com.umc.product.notification.domain.EmailTemplateType;
import com.umc.product.notification.domain.exception.EmailDomainException;
import com.umc.product.notification.domain.exception.EmailErrorCode;
import com.umc.product.support.IntegrationTestSupport;

@DisplayName("Template email outbox 수동 relay 통합")
@TestPropertySource(properties = {
    "app.event-outbox.relay-enabled=false",
    "app.event-outbox.max-attempts=2"
})
class TemplateEmailOutboxRelayIntegrationTest extends IntegrationTestSupport {

    private static final String RAW_EMAIL = "applicant@test.umc.local";
    private static final String RAW_VARIABLE = "지원자-민감정보-1147";

    @Autowired
    private SendEmailUseCase sendEmailUseCase;

    @Autowired
    private EventOutboxRelayService relayService;

    @Autowired
    private EventOutboxJpaRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private SendEmailPort sendEmailPort;

    @Test
    @DisplayName("due 요청은 transaction 밖에서 한 번 발송하고 PUBLISHED·중복 무발송을 보장한다")
    void due_요청을_한번_발송하고_중복은_재발송하지_않는다() {
        AtomicBoolean transactionActive = new AtomicBoolean(true);
        doAnswer(invocation -> {
            transactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
            return null;
        }).when(sendEmailPort).send(any(EmailMessage.class));
        Instant availableAt = Instant.now().minusSeconds(1);
        SendTemplateEmailCommand command = command(
            UUID.fromString("70000000-0000-0000-0000-000000000101"),
            availableAt
        );

        TemplateEmailRequestInfo first = sendEmailUseCase.requestTemplateEmail(command);
        relayService.relay();
        TemplateEmailRequestInfo duplicate = sendEmailUseCase.requestTemplateEmail(command);
        relayService.relay();

        ArgumentCaptor<EmailMessage> messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(sendEmailPort, times(1)).send(messageCaptor.capture());
        EmailMessage message = messageCaptor.getValue();
        assertThat(first.deduplicated()).isFalse();
        assertThat(duplicate.deduplicated()).isTrue();
        assertThat(duplicate.status()).isEqualTo(EventOutboxStatus.PUBLISHED);
        assertThat(first.availableAt()).isEqualTo(availableAt.truncatedTo(ChronoUnit.MICROS));
        assertThat(state(command.eventId()).status()).isEqualTo("PUBLISHED");
        assertThat(transactionActive).isFalse();
        assertThat(message.fromAddress()).isEqualTo("noreply@test.umc.local");
        assertThat(message.subject()).contains("서류 전형 합격");
        assertThat(message.htmlBody()).contains(RAW_VARIABLE, "면접 가능 시간 제출하기");
        assertThat(applicationContext.getBeansOfType(EventOutboxPoller.class)).isEmpty();
        System.out.printf(
            "TODO7_RELAY_SUCCESS status=PUBLISHED sends=1 duplicate=%s transactionActive=%s from=%s%n",
            duplicate.deduplicated(),
            transactionActive.get(),
            message.fromAddress()
        );
    }

    @Test
    @DisplayName("future 요청은 발송하지 않고 PENDING으로 유지한다")
    void future_요청은_due_이전까지_발송하지_않는다() {
        SendTemplateEmailCommand command = command(
            UUID.fromString("70000000-0000-0000-0000-000000000102"),
            Instant.now().plusSeconds(3_600)
        );

        sendEmailUseCase.requestTemplateEmail(command);
        relayService.relay();

        verifyNoInteractions(sendEmailPort);
        assertThat(state(command.eventId()).status()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("provider 실패는 stable code와 backoff를 기록하고 다음 due relay에서 성공한다")
    void provider_실패를_backoff_후_재시도해_성공한다() {
        EmailDomainException failure = failure();
        doThrow(failure).doNothing().when(sendEmailPort).send(any(EmailMessage.class));
        SendTemplateEmailCommand command = command(
            UUID.fromString("70000000-0000-0000-0000-000000000103"),
            Instant.now().minusSeconds(1)
        );

        sendEmailUseCase.requestTemplateEmail(command);
        relayService.relay();
        OutboxState failedOnce = state(command.eventId());
        makeDue(command.eventId());
        relayService.relay();

        assertThat(failedOnce.status()).isEqualTo("PENDING");
        assertThat(failedOnce.attempts()).isOne();
        assertThat(failedOnce.lastError()).isEqualTo("EMAIL-0005");
        assertThat(failedOnce.nextAttemptAt()).isAfter(Instant.now());
        assertThat(state(command.eventId()).status()).isEqualTo("PUBLISHED");
        assertThat(piiInLastErrorCount()).isZero();
        verify(sendEmailPort, times(2)).send(any(EmailMessage.class));
        System.out.println("TODO7_RELAY_RETRY first=PENDING attempts=1 lastError=EMAIL-0005 retry=PUBLISHED");
    }

    @Test
    @DisplayName("provider가 최대 횟수까지 실패하면 PII 없이 FAILED로 종료한다")
    void provider_최대_실패는_FAILED로_종료한다() {
        doThrow(failure()).when(sendEmailPort).send(any(EmailMessage.class));
        SendTemplateEmailCommand command = command(
            UUID.fromString("70000000-0000-0000-0000-000000000104"),
            Instant.now().minusSeconds(1)
        );

        sendEmailUseCase.requestTemplateEmail(command);
        relayService.relay();
        makeDue(command.eventId());
        relayService.relay();
        relayService.relay();

        OutboxState state = state(command.eventId());
        assertThat(state.status()).isEqualTo("FAILED");
        assertThat(state.attempts()).isEqualTo(2);
        assertThat(state.lastError()).isEqualTo("EMAIL-0005");
        assertThat(piiInLastErrorCount()).isZero();
        verify(sendEmailPort, times(2)).send(any(EmailMessage.class));
        System.out.println("TODO7_RELAY_MAX status=FAILED attempts=2 lastError=EMAIL-0005 pii=false");
    }

    private SendTemplateEmailCommand command(UUID eventId, Instant availableAt) {
        return new SendTemplateEmailCommand(
            eventId,
            RAW_EMAIL,
            EmailTemplateType.RECRUITMENT_DOCUMENT_PASSED_INTERVIEW_AVAILABILITY_REQUEST,
            Map.of(
                "applicantName", RAW_VARIABLE,
                "contactSnapshot", "contact@university.neordinary.com",
                "actionUrl", "https://university.neordinary.com/interview?slot=1"
            ),
            availableAt
        );
    }

    private EmailDomainException failure() {
        return new EmailDomainException(
            EmailErrorCode.EMAIL_SEND_FAILED,
            new IllegalStateException(RAW_EMAIL + " " + RAW_VARIABLE)
        );
    }

    private void makeDue(UUID eventId) {
        jdbcTemplate.update(
            "UPDATE event_outbox SET next_attempt_at = CURRENT_TIMESTAMP - INTERVAL '1 second' WHERE event_id = ?",
            eventId
        );
    }

    private long piiInLastErrorCount() {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM event_outbox WHERE last_error LIKE ? OR last_error LIKE ?",
            Long.class,
            "%" + RAW_EMAIL + "%",
            "%" + RAW_VARIABLE + "%"
        );
    }

    private OutboxState state(UUID eventId) {
        return jdbcTemplate.queryForObject(
            "SELECT status, attempts, last_error, next_attempt_at FROM event_outbox WHERE event_id = ?",
            (resultSet, rowNumber) -> new OutboxState(
                resultSet.getString("status"),
                resultSet.getInt("attempts"),
                resultSet.getString("last_error"),
                resultSet.getTimestamp("next_attempt_at").toInstant()
            ),
            eventId
        );
    }

    private record OutboxState(String status, int attempts, String lastError, Instant nextAttemptAt) {
    }
}
