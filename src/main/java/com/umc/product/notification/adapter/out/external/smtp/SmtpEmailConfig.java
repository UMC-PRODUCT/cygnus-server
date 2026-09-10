package com.umc.product.notification.adapter.out.external.smtp;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
@ConditionalOnProperty(prefix = "app.notification.email", name = "provider", havingValue = "smtp")
@EnableConfigurationProperties(SmtpProperties.class)
public class SmtpEmailConfig {

    @Bean
    public JavaMailSenderImpl smtpMailSender(SmtpProperties properties) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(properties.host());
        sender.setPort(properties.port());
        sender.setUsername(properties.username());
        sender.setPassword(properties.password());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        // 평문 인증과 인증서 검증 우회를 허용하지 않는다. 응답 없는 SMTP 서버가 작업 스레드를 점유하지 않게 한다.
        Properties mailProperties = sender.getJavaMailProperties();
        mailProperties.setProperty("mail.smtp.auth", "true");
        mailProperties.setProperty("mail.smtp.starttls.enable", "true");
        mailProperties.setProperty("mail.smtp.starttls.required", "true");
        mailProperties.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        mailProperties.setProperty("mail.smtp.connectiontimeout", "5000");
        mailProperties.setProperty("mail.smtp.timeout", "5000");
        mailProperties.setProperty("mail.smtp.writetimeout", "5000");
        mailProperties.setProperty("mail.debug", "false");
        sender.getSession().setDebug(false);
        return sender;
    }
}
