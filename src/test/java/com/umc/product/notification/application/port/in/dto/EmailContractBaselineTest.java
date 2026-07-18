package com.umc.product.notification.application.port.in.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import com.umc.product.notification.application.service.EmailSenderProperties;

class EmailContractBaselineTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(EmailSenderPropertiesTestConfiguration.class)
        .withPropertyValues(
            "app.notification.email.sender.no-reply-address=noreply@test.umc.local",
            "app.notification.email.sender.no-reply-display-name=UMC Test"
        );

    @Test
    @DisplayName("기존 HTML 이메일 command는 수신자·제목·본문 계약을 유지한다")
    void 기존_html_이메일_command_계약을_유지한다() {
        SendHtmlEmailCommand command = new SendHtmlEmailCommand(
            "receiver@test.umc.local",
            "제목",
            "<p>본문</p>"
        );

        assertThat(command.to()).isEqualTo("receiver@test.umc.local");
        assertThat(command.subject()).isEqualTo("제목");
        assertThat(command.htmlContent()).isEqualTo("<p>본문</p>");
    }

    @Test
    @DisplayName("기존 verification 이메일 발신자 설정 binding은 유지한다")
    void 기존_verification_이메일_발신자_설정_binding을_유지한다() {
        contextRunner.run(context -> {
            EmailSenderProperties properties = context.getBean(EmailSenderProperties.class);

            assertThat(properties.noReplyAddress()).isEqualTo("noreply@test.umc.local");
            assertThat(properties.noReplyDisplayName()).isEqualTo("UMC Test");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EmailSenderProperties.class)
    static class EmailSenderPropertiesTestConfiguration {
    }
}
