package com.umc.product.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.umc.product.notification.application.port.in.dto.SendHtmlEmailCommand;
import com.umc.product.notification.application.port.in.dto.SendVerificationEmailCommand;
import com.umc.product.notification.application.port.out.SendEmailPort;
import com.umc.product.notification.application.port.out.dto.EmailMessage;
import com.umc.product.notification.domain.exception.EmailDomainException;
import com.umc.product.notification.domain.exception.EmailErrorCode;

@ExtendWith(MockitoExtension.class)
class SendEmailServiceTest {

    @Mock
    TemplateEngine templateEngine;
    @Mock
    SendEmailPort sendEmailPort;

    SendEmailService sut;

    @BeforeEach
    void setUp() {
        sut = new SendEmailService(
            templateEngine,
            sendEmailPort,
            new EmailSenderProperties("no-reply@example.org", "UMC")
        );
    }

    @Test
    @DisplayName("HTML 이메일은 지정한 Thymeleaf template과 변수로 렌더링해 발송한다")
    void HTML_이메일은_Thymeleaf_template으로_렌더링한다() {
        given(templateEngine.process(eq("email/recruiting-interview-availability"), any(Context.class)))
            .willReturn("<html>면접 일정</html>");

        sut.sendHtmlEmail(new SendHtmlEmailCommand(
            "applicant@example.org",
            "면접 일정 요청",
            "email/recruiting-interview-availability",
            Map.of("applicantName", "지원자")
        ));

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        then(templateEngine).should().process(
            eq("email/recruiting-interview-availability"),
            contextCaptor.capture()
        );
        assertThat(contextCaptor.getValue().getVariable("applicantName")).isEqualTo("지원자");
        ArgumentCaptor<EmailMessage> messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        then(sendEmailPort).should().send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().htmlBody()).isEqualTo("<html>면접 일정</html>");
        assertThat(messageCaptor.getValue().subject()).isEqualTo("면접 일정 요청");
    }

    @Test
    @DisplayName("인증 이메일은 고정 template과 인증 코드 제목으로 발송한다")
    void verification_email_uses_fixed_template_and_subject() {
        given(templateEngine.process(eq("email/verification"), any(Context.class))).willReturn("<html>123456</html>");

        sut.sendVerificationEmail(new SendVerificationEmailCommand("member@example.org", "123456"));

        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        then(sendEmailPort).should().send(captor.capture());
        assertThat(captor.getValue().subject()).isEqualTo("이메일 인증 코드: 123456");
        assertThat(captor.getValue().fromAddress()).isEqualTo("no-reply@example.org");
    }

    @Test
    @DisplayName("인증·HTML template 렌더링 실패는 원인을 보존한 도메인 예외로 변환한다")
    void template_failures_are_wrapped() {
        given(templateEngine.process(eq("email/verification"), any(Context.class)))
            .willThrow(new IllegalStateException("verification render"));
        assertThatThrownBy(() -> sut.sendVerificationEmail(new SendVerificationEmailCommand(null, "123456")))
            .isInstanceOf(EmailDomainException.class)
            .hasCauseInstanceOf(IllegalStateException.class);

        given(templateEngine.process(eq("broken"), any(Context.class)))
            .willThrow(new IllegalStateException("html render"));
        assertThatThrownBy(() -> sut.sendHtmlEmail(new SendHtmlEmailCommand(" ", "제목", "broken", Map.of())))
            .isInstanceOf(EmailDomainException.class)
            .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("인증·HTML 발송 실패는 삼키지 않고 호출자에게 전파한다")
    void send_failures_are_propagated() {
        given(templateEngine.process(any(String.class), any(Context.class))).willReturn("html");
        EmailDomainException failure = new EmailDomainException(EmailErrorCode.EMAIL_SEND_FAILED);
        doThrow(failure).when(sendEmailPort).send(any());

        assertThatThrownBy(() -> sut.sendVerificationEmail(new SendVerificationEmailCommand("member@example.org", "1")))
            .isSameAs(failure);
        assertThatThrownBy(() -> sut.sendHtmlEmail(new SendHtmlEmailCommand(
            "member@example.org", "제목", "template", Map.of()
        ))).isSameAs(failure);
    }
}
