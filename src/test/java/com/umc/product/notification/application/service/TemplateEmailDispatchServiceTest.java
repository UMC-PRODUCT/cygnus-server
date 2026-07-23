package com.umc.product.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.umc.product.notification.application.port.out.SendEmailPort;
import com.umc.product.notification.application.port.out.dto.EmailMessage;
import com.umc.product.notification.domain.EmailTemplateType;
import com.umc.product.notification.domain.TemplateEmailRequestedEvent;
import com.umc.product.notification.domain.exception.EmailDomainException;
import com.umc.product.notification.domain.exception.EmailErrorCode;

@DisplayName("Template email 동기 dispatch 서비스")
class TemplateEmailDispatchServiceTest {

    private static final String RAW_PII = "홍길동 연락처 010-1234-5678";

    @Test
    @DisplayName("catalog의 제목과 resource path로 실제 Thymeleaf 본문을 렌더링해 동기로 발송한다")
    void testCase001() {
        SendEmailPort sendEmailPort = mock(SendEmailPort.class);
        TemplateEmailDispatchService service = service(templateEngine(), sendEmailPort);

        service.deliver(event(
            EmailTemplateType.RECRUITMENT_DOCUMENT_PASSED_INTERVIEW_AVAILABILITY_REQUEST,
            Map.of(
                "applicantName", "홍길동",
                "contactSnapshot", "채용 문의 contact@university.neordinary.com",
                "actionUrl", "https://university.neordinary.com/interview?slot=1"
            )
        ));

        ArgumentCaptor<EmailMessage> messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(sendEmailPort).send(messageCaptor.capture());
        EmailMessage message = messageCaptor.getValue();
        assertThat(message.fromAddress()).isEqualTo("noreply@test.umc.local");
        assertThat(message.fromDisplayName()).isEqualTo("UMC 테스트");
        assertThat(message.to()).isEqualTo("applicant@test.umc.local");
        assertThat(message.subject()).isEqualTo("[UMC] 서류 전형 합격 및 면접 가능 시간 제출 안내");
        assertThat(message.htmlBody()).contains("홍길동", "면접 가능 시간 제출하기");
        assertThat(message.subject().getBytes(StandardCharsets.UTF_8)).isNotEmpty();
        assertThat(message.htmlBody().getBytes(StandardCharsets.UTF_8)).isNotEmpty();
    }

    @Test
    @DisplayName("렌더링 실패는 PII 없는 안정적인 EMAIL render code로 전파한다")
    void testCase002() {
        TemplateEngine failingTemplateEngine = mock(TemplateEngine.class);
        given(failingTemplateEngine.process(any(String.class), any())).willThrow(new IllegalStateException(RAW_PII));
        TemplateEmailDispatchService service = service(failingTemplateEngine, mock(SendEmailPort.class));

        assertThatThrownBy(() -> service.deliver(event(
            EmailTemplateType.RECRUITMENT_FINAL_FAILED,
            Map.of("applicantName", "민감한 지원 정보")
        )))
            .isInstanceOf(EmailDomainException.class)
            .satisfies(exception -> {
                EmailDomainException emailException = (EmailDomainException) exception;
                assertThat(emailException.getBaseCode()).isEqualTo(EmailErrorCode.EMAIL_TEMPLATE_RENDER_FAILED);
                assertThat(String.valueOf(emailException.getMessage()))
                    .doesNotContain(RAW_PII, "applicant@test.umc.local");
                assertThat(emailException.getCause())
                    .as("telemetry error에 도달 가능한 raw render cause")
                    .isNull();
            });
    }

    @Test
    @DisplayName("provider runtime 실패는 PII 없는 EMAIL send code로 전파한다")
    void testCase003() {
        TemplateEngine templateEngine = mock(TemplateEngine.class);
        given(templateEngine.process(any(String.class), any())).willReturn("<html>본문</html>");
        SendEmailPort sendEmailPort = mock(SendEmailPort.class);
        willThrow(new IllegalStateException(RAW_PII)).given(sendEmailPort).send(any(EmailMessage.class));
        TemplateEmailDispatchService service = service(templateEngine, sendEmailPort);

        assertThatThrownBy(() -> service.deliver(event(
            EmailTemplateType.RECRUITMENT_FINAL_FAILED,
            Map.of("applicantName", "민감한 지원 정보")
        )))
            .isInstanceOf(EmailDomainException.class)
            .satisfies(exception -> {
                EmailDomainException emailException = (EmailDomainException) exception;
                assertThat(emailException.getBaseCode()).isEqualTo(EmailErrorCode.EMAIL_SEND_FAILED);
                assertThat(String.valueOf(emailException.getMessage()))
                    .doesNotContain(RAW_PII, "applicant@test.umc.local");
                assertThat(emailException.getCause())
                    .as("telemetry error에 도달 가능한 raw provider cause")
                    .isNull();
            });
    }

    @Test
    @DisplayName("provider 도메인 오류도 cause를 제거하고 재시도 여부만 보존한다")
    void testCase004() {
        TemplateEngine templateEngine = mock(TemplateEngine.class);
        given(templateEngine.process(any(String.class), any())).willReturn("<html>본문</html>");
        SendEmailPort sendEmailPort = mock(SendEmailPort.class);
        willThrow(new EmailDomainException(
            EmailErrorCode.EMAIL_SEND_FAILED,
            false,
            new IllegalStateException(RAW_PII)
        )).given(sendEmailPort).send(any(EmailMessage.class));
        TemplateEmailDispatchService service = service(templateEngine, sendEmailPort);

        assertThatThrownBy(() -> service.deliver(event(
            EmailTemplateType.RECRUITMENT_FINAL_FAILED,
            Map.of("applicantName", "민감한 지원 정보")
        )))
            .isInstanceOf(EmailDomainException.class)
            .satisfies(exception -> {
                EmailDomainException emailException = (EmailDomainException) exception;
                assertThat(emailException.getBaseCode()).isEqualTo(EmailErrorCode.EMAIL_SEND_FAILED);
                assertThat(emailException.retryable()).isFalse();
                assertThat(emailException.getCause()).isNull();
                assertThat(String.valueOf(emailException.getMessage())).doesNotContain(RAW_PII);
            });
    }

    private TemplateEmailDispatchService service(TemplateEngine templateEngine, SendEmailPort sendEmailPort) {
        return new TemplateEmailDispatchService(
            templateEngine,
            sendEmailPort,
            new EmailTemplateCatalog(List.of("https://university.neordinary.com")),
            new EmailSenderProperties("noreply@test.umc.local", "UMC 테스트")
        );
    }

    private TemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private TemplateEmailRequestedEvent event(EmailTemplateType type, Map<String, String> variables) {
        return new TemplateEmailRequestedEvent(
            UUID.fromString("70000000-0000-0000-0000-000000000002"),
            Instant.parse("2026-07-18T00:00:00Z"),
            "applicant@test.umc.local",
            type,
            variables
        );
    }
}
