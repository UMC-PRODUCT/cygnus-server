package com.umc.product.notification.adapter.out.external.smtp;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.umc.product.global.logging.ExternalApiCallLogger;
import com.umc.product.notification.application.port.out.SendEmailPort;
import com.umc.product.notification.application.port.out.dto.EmailMessage;
import com.umc.product.notification.domain.exception.EmailDomainException;
import com.umc.product.notification.domain.exception.EmailErrorCode;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.notification.email", name = "provider", havingValue = "smtp")
@RequiredArgsConstructor
public class SmtpEmailAdapter implements SendEmailPort {

    private final JavaMailSender mailSender;

    @Override
    public void send(EmailMessage message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
            helper.setFrom(message.fromAddress(), message.fromDisplayName());
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.htmlBody(), true);
            ExternalApiCallLogger.measure("SMTP", "SEND_EMAIL", () -> mailSender.send(mimeMessage));
        } catch (MessagingException | UnsupportedEncodingException | RuntimeException e) {
            // SMTP 예외에는 수신 주소와 서버 응답이 포함될 수 있어 원문이나 cause를 상위 로그로 전달하지 않는다.
            log.warn("SMTP 이메일 발송 실패: errorClass={}", e.getClass().getSimpleName());
            throw new EmailDomainException(EmailErrorCode.EMAIL_SEND_FAILED);
        }
    }
}
