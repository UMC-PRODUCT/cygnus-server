package com.umc.product.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
import org.thymeleaf.TemplateEngine;

import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.event.application.port.out.dto.OutboxPublishResult;
import com.umc.product.global.event.domain.EventOutboxStatus;
import com.umc.product.global.event.domain.OutboxDispatchMode;
import com.umc.product.notification.application.port.in.dto.SendTemplateEmailCommand;
import com.umc.product.notification.application.port.in.dto.TemplateEmailRequestInfo;
import com.umc.product.notification.application.port.out.SendEmailPort;
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

}
