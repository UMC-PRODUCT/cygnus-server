package com.umc.product.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
    @DisplayName("인증메일은 링크 없는 순수 텍스트로 발송하고 제목에 인증코드를 포함하지 않는다")
    void 인증메일은_링크_없는_텍스트로_발송한다() {
        // given
        SendVerificationEmailCommand command = SendVerificationEmailCommand.builder()
            .to("recipient@example.org")
            .verificationCode("123456")
            .build();

        // when
        sut.sendVerificationEmail(command);

        // then
        ArgumentCaptor<EmailMessage> messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
        then(sendEmailPort).should().send(messageCaptor.capture());
        then(templateEngine).shouldHaveNoInteractions();
        EmailMessage message = messageCaptor.getValue();
        assertThat(message.fromAddress()).isEqualTo("no-reply@example.org");
        assertThat(message.fromDisplayName()).isEqualTo("UMC");
        assertThat(message.to()).isEqualTo("recipient@example.org");
        assertThat(message.subject()).isEqualTo("[UMC] 이메일 인증 코드").doesNotContain("123456");
        assertThat(message.html()).isFalse();
        assertThat(message.body()).contains("UMC", "인증 코드: 123456", "요청 시점부터 10분", "직접 요청하지 않았다면")
            .doesNotContain("<", ">", "http://", "https://", "개인정보 처리방침", "이용약관");
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
        assertThat(messageCaptor.getValue().body()).isEqualTo("<html>면접 일정</html>");
        assertThat(messageCaptor.getValue().html()).isTrue();
        assertThat(messageCaptor.getValue().subject()).isEqualTo("면접 일정 요청");
    }
}
