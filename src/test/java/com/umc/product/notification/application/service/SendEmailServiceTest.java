package com.umc.product.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.event.application.port.out.dto.OutboxPublishResult;
import com.umc.product.global.event.domain.EventOutboxStatus;
import com.umc.product.global.event.domain.OutboxDispatchMode;
import com.umc.product.notification.application.port.in.dto.SendTemplateEmailCommand;
import com.umc.product.notification.application.port.in.dto.SendVerificationEmailCommand;
import com.umc.product.notification.application.port.in.dto.TemplateEmailRequestInfo;
import com.umc.product.notification.application.port.out.SendEmailPort;
import com.umc.product.notification.application.port.out.dto.EmailMessage;
import com.umc.product.notification.domain.EmailTemplateType;
import com.umc.product.notification.domain.TemplateEmailRequestedEvent;

@DisplayName("이메일 발송 서비스")
@ExtendWith(MockitoExtension.class)
class SendEmailServiceTest {

    private static final UUID EVENT_ID = UUID.fromString("70000000-0000-0000-0000-000000000001");
    private static final Instant AVAILABLE_AT = Instant.parse("2026-07-18T03:00:00.123456789Z");

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private SendEmailPort sendEmailPort;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private SendEmailService service;

    @BeforeEach
    void setUp() {
        service = new SendEmailService(
            templateEngine,
            sendEmailPort,
            new EmailSenderProperties("noreply@test.umc.local", "UMC 테스트"),
            new EmailTemplateCatalog(List.of("https://university.neordinary.com")),
            domainEventPublisher
        );
    }

    @Test
    @DisplayName("template email 요청은 검증된 불변 event를 publishOnce하고 outbox 결과를 반환한다")
    void template_email_요청을_outbox에_한번_기록한다() {
        SendTemplateEmailCommand command = new SendTemplateEmailCommand(
            EVENT_ID,
            " applicant@test.umc.local ",
            EmailTemplateType.RECRUITMENT_FINAL_FAILED,
            Map.of(" applicantName ", " 홍길동 "),
            AVAILABLE_AT
        );
        Instant normalizedAvailableAt = AVAILABLE_AT.truncatedTo(ChronoUnit.MICROS);
        given(domainEventPublisher.publishOnce(any(TemplateEmailRequestedEvent.class), eq(AVAILABLE_AT)))
            .willReturn(new OutboxPublishResult(
                EVENT_ID,
                EventOutboxStatus.PENDING,
                false,
                normalizedAvailableAt
            ));
        Instant beforeRequest = Instant.now();

        TemplateEmailRequestInfo result = service.requestTemplateEmail(command);

        Instant afterRequest = Instant.now();
        ArgumentCaptor<TemplateEmailRequestedEvent> eventCaptor =
            ArgumentCaptor.forClass(TemplateEmailRequestedEvent.class);
        verify(domainEventPublisher).publishOnce(eventCaptor.capture(), eq(AVAILABLE_AT));
        TemplateEmailRequestedEvent event = eventCaptor.getValue();
        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.occurredAt()).isBetween(beforeRequest, afterRequest);
        assertThat(event.recipient()).isEqualTo("applicant@test.umc.local");
        assertThat(event.type()).isEqualTo(EmailTemplateType.RECRUITMENT_FINAL_FAILED);
        assertThat(event.variables()).containsExactlyEntriesOf(Map.of("applicantName", "홍길동"));
        assertThat(event.variables()).isUnmodifiable();
        assertThat(event.outboxDispatchMode()).isEqualTo(OutboxDispatchMode.NON_TRANSACTIONAL);
        assertThat(result).isEqualTo(new TemplateEmailRequestInfo(
            EVENT_ID,
            EventOutboxStatus.PENDING,
            false,
            normalizedAvailableAt,
            normalizedAvailableAt
        ));
    }

    @Test
    @DisplayName("기존 verification 발송은 emailTaskExecutor 비동기 계약과 발신 형식을 유지한다")
    void verification_이메일의_기존_비동기_동작을_유지한다() throws Exception {
        given(templateEngine.process(eq("email/verification"), any(Context.class))).willReturn("<html>인증</html>");
        SendVerificationEmailCommand command = SendVerificationEmailCommand.builder()
            .to("member@test.umc.local")
            .verificationCode("123456")
            .build();

        service.sendVerificationEmail(command);

        ArgumentCaptor<EmailMessage> messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(sendEmailPort).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue()).isEqualTo(new EmailMessage(
            "noreply@test.umc.local",
            "UMC 테스트",
            "member@test.umc.local",
            "이메일 인증 코드: 123456",
            "<html>인증</html>"
        ));
        Method verificationMethod = SendEmailService.class.getMethod(
            "sendVerificationEmail",
            SendVerificationEmailCommand.class
        );
        assertThat(verificationMethod.getAnnotation(Async.class).value()).isEqualTo("emailTaskExecutor");
    }

    @Test
    @DisplayName("template email 요청 method는 transaction 경계를 갖는다")
    void template_email_요청은_transaction_경계를_갖는다() throws Exception {
        Method requestMethod = SendEmailService.class.getMethod(
            "requestTemplateEmail",
            SendTemplateEmailCommand.class
        );

        assertThat(requestMethod.getAnnotation(Transactional.class)).isNotNull();
        assertThat(requestMethod.getAnnotation(Async.class)).isNull();
    }
}
