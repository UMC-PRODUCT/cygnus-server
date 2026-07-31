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
}
