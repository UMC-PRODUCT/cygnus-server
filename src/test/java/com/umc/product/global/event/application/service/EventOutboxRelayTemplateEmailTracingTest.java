package com.umc.product.global.event.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.thymeleaf.TemplateEngine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.global.event.adapter.out.EventPayloadDeserializer;
import com.umc.product.global.event.adapter.out.EventPayloadSerializer;
import com.umc.product.global.event.application.service.EventOutboxRelayTestFixtures.FakeLoadEventOutboxPort;
import com.umc.product.global.event.application.service.EventOutboxRelayTestFixtures.FakeSaveEventOutboxPort;
import com.umc.product.global.event.application.service.EventOutboxRelayTestFixtures.LocalTransactionManager;
import com.umc.product.global.event.domain.EventOutbox;
import com.umc.product.notification.application.port.out.SendEmailPort;
import com.umc.product.notification.application.service.EmailSenderProperties;
import com.umc.product.notification.application.service.EmailTemplateCatalog;
import com.umc.product.notification.application.service.TemplateEmailDispatchService;
import com.umc.product.notification.domain.EmailTemplateType;
import com.umc.product.notification.domain.TemplateEmailRequestedEvent;
import com.umc.product.notification.domain.exception.EmailDomainException;
import com.umc.product.notification.domain.exception.EmailErrorCode;

import io.micrometer.tracing.test.simple.SimpleSpan;
import io.micrometer.tracing.test.simple.SimpleTracer;

@DisplayName("Template email outbox relay tracing")
class EventOutboxRelayTemplateEmailTracingTest {

    private static final String RAW_EMAIL = "applicant-secret@example.com";
    private static final String RAW_VARIABLE = "지원자-민감정보-010-1234-5678";

    @Test
    @DisplayName("cause-bearing provider 실패가 relay error span에 PII를 남기지 않는다")
    void testCase001() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        EventPayloadSerializer serializer = new EventPayloadSerializer(objectMapper);
        TemplateEmailRequestedEvent event = new TemplateEmailRequestedEvent(
            UUID.randomUUID(),
            Instant.now(),
            RAW_EMAIL,
            EmailTemplateType.RECRUITMENT_FINAL_FAILED,
            Map.of("applicantName", RAW_VARIABLE)
        );
        EventOutbox outbox = EventOutbox.record(event, serializer.serialize(event));
        TemplateEngine templateEngine = mock(TemplateEngine.class);
        given(templateEngine.process(any(String.class), any())).willReturn("<html>본문</html>");
        SendEmailPort sendEmailPort = ignored -> {
            throw new EmailDomainException(
                EmailErrorCode.EMAIL_SEND_FAILED,
                new IllegalStateException(RAW_EMAIL + " " + RAW_VARIABLE)
            );
        };
        TemplateEmailDispatchService dispatchService = new TemplateEmailDispatchService(
            templateEngine,
            sendEmailPort,
            new EmailTemplateCatalog(List.of("https://university.neordinary.com")),
            new EmailSenderProperties("noreply@test.umc.local", "UMC 테스트")
        );
        ApplicationEventPublisher publisher = delivered ->
            dispatchService.deliver((TemplateEmailRequestedEvent) delivered);
        SimpleTracer tracer = new SimpleTracer();
        EventOutboxRelayService relayService = new EventOutboxRelayService(
            new FakeLoadEventOutboxPort(List.of(outbox)),
            new FakeSaveEventOutboxPort(),
            new EventPayloadDeserializer(objectMapper),
            publisher,
            new LocalTransactionManager(),
            tracer,
            1,
            3
        );

        relayService.relay();

        SimpleSpan relaySpan = tracer.getSpans().stream()
            .filter(span -> "outbox.relay.publish".equals(span.getName()))
            .findFirst()
            .orElseThrow();
        assertThat(relaySpan.getError()).isInstanceOf(EmailDomainException.class);
        assertThat(relaySpan.getError().getCause()).isNull();
        assertThat(String.valueOf(relaySpan.getError())).doesNotContain(RAW_EMAIL, RAW_VARIABLE);
    }
}
