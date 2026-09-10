package com.umc.product.notification.adapter.out.external;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.umc.product.notification.adapter.out.external.ses.SesEmailAdapter;
import com.umc.product.notification.adapter.out.external.ses.SesEmailConfig;
import com.umc.product.notification.adapter.out.external.ses.SesProperties;
import com.umc.product.notification.adapter.out.external.smtp.SmtpEmailAdapter;
import com.umc.product.notification.adapter.out.external.smtp.SmtpEmailConfig;
import com.umc.product.notification.adapter.out.external.smtp.SmtpProperties;
import com.umc.product.notification.application.port.out.SendEmailPort;

import software.amazon.awssdk.services.sesv2.SesV2Client;

class EmailProviderConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(EmailConfiguration.class);

    @Test
    @DisplayName("기본 제공자는 SES이고 SMTP 자격증명 없이 부팅한다")
    void default_provider_uses_ses_without_smtp_credentials() {
        // given / when
        contextRunner.withPropertyValues(
            "app.notification.email.ses.region=ap-northeast-2",
            "app.notification.email.ses.access-key-id=test-access-key",
            "app.notification.email.ses.secret-access-key=test-secret-key"
        ).run(context -> {
            // then
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SendEmailPort.class);
            assertThat(context).hasSingleBean(SesEmailAdapter.class);
            assertThat(context).hasSingleBean(SesV2Client.class);
            assertThat(context).doesNotHaveBean(SmtpProperties.class);
            assertThat(context).doesNotHaveBean(JavaMailSender.class);
            assertThat(context.getBean(EmailProviderProperties.class).provider())
                .isEqualTo(EmailProviderProperties.Provider.SES);
        });
    }

    @Test
    @DisplayName("SMTP 제공자는 SES 자격증명 없이 부팅하고 TLS 및 timeout을 강제한다")
    void smtp_provider_boots_without_ses_credentials() {
        // given / when
        contextRunner.withPropertyValues(
            "app.notification.email.provider=smtp",
            "app.notification.email.smtp.username=sender@example.org",
            "app.notification.email.smtp.password=test-app-password"
        ).run(context -> {
            // then
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SendEmailPort.class);
            assertThat(context).hasSingleBean(SmtpEmailAdapter.class);
            assertThat(context).doesNotHaveBean(SesProperties.class);
            assertThat(context).doesNotHaveBean(SesV2Client.class);
            assertThat(context).doesNotHaveBean(SesEmailAdapter.class);
            JavaMailSenderImpl sender = context.getBean(JavaMailSenderImpl.class);
            assertThat(sender.getHost()).isEqualTo("smtp.gmail.com");
            assertThat(sender.getPort()).isEqualTo(587);
            assertThat(sender.getJavaMailProperties())
                .containsEntry("mail.smtp.auth", "true")
                .containsEntry("mail.smtp.starttls.enable", "true")
                .containsEntry("mail.smtp.starttls.required", "true")
                .containsEntry("mail.smtp.ssl.checkserveridentity", "true")
                .containsEntry("mail.smtp.connectiontimeout", "5000")
                .containsEntry("mail.smtp.timeout", "5000")
                .containsEntry("mail.smtp.writetimeout", "5000")
                .doesNotContainKey("mail.smtp.ssl.trust");
            assertThat(sender.getSession().getDebug()).isFalse();
            assertThat(context.getBean(SmtpProperties.class).toString())
                .doesNotContain("sender@example.org", "test-app-password");
        });
    }

    @Test
    @DisplayName("잘못된 이메일 제공자는 설정 바인딩 오류로 부팅을 막는다")
    void unsupported_provider_fails_at_startup() {
        // given / when
        contextRunner.withPropertyValues("app.notification.email.provider=unsupported").run(context -> {
            // then
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("app.notification.email.provider");
        });
    }

    @Test
    @DisplayName("SMTP를 선택하고 비밀번호를 누락하면 부팅을 막는다")
    void smtp_requires_password() {
        // given / when
        contextRunner.withPropertyValues(
            "app.notification.email.provider=smtp",
            "app.notification.email.smtp.username=sender@example.org"
        ).run(context -> {
            // then
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("password");
        });
    }

    @Test
    @DisplayName("SES 선택 시 기존 자격증명 필수 검증을 유지한다")
    void ses_still_requires_credentials() {
        // given / when
        contextRunner.withPropertyValues(
            "app.notification.email.provider=ses",
            "app.notification.email.ses.region=ap-northeast-2"
        ).run(context -> {
            // then
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("accessKeyId");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @ConfigurationPropertiesScan(basePackageClasses = EmailProviderProperties.class)
    @Import({SesEmailConfig.class, SesEmailAdapter.class, SmtpEmailConfig.class, SmtpEmailAdapter.class})
    static class EmailConfiguration {
    }
}
