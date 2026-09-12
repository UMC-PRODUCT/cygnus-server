package com.umc.product.notification.adapter.out.external.smtp;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.umc.product.global.logging.ExternalApiCallLogger;
import com.umc.product.notification.application.port.out.SendEmailPort;
import com.umc.product.notification.application.port.out.dto.EmailMessage;
import com.umc.product.notification.domain.exception.EmailDomainException;
import com.umc.product.notification.domain.exception.EmailErrorCode;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.notification.email", name = "provider", havingValue = "smtp")
public class SmtpEmailAdapter implements SendEmailPort {

    private final JavaMailSender mailSender;
    private final Tracer tracer;

    @Autowired
    public SmtpEmailAdapter(JavaMailSender mailSender, ObjectProvider<Tracer> tracerProvider) {
        this.mailSender = mailSender;
        this.tracer = tracerProvider.getIfAvailable(() -> Tracer.NOOP);
    }

    public SmtpEmailAdapter(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        this.tracer = Tracer.NOOP;
    }

    @Override
    public void send(EmailMessage message) {
        long startNanos = System.nanoTime();
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
            helper.setFrom(message.fromAddress(), message.fromDisplayName());
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.htmlBody(), true);
            ExternalApiCallLogger.measure("SMTP", "SEND_EMAIL", () -> mailSender.send(mimeMessage));
        } catch (MessagingException | UnsupportedEncodingException | RuntimeException e) {
            recordFailure(e, (System.nanoTime() - startNanos) / 1_000_000L);
            // 수신 주소·인증코드를 포함할 수 있는 원문과 cause는 외부 응답이나 상위 예외에 전달하지 않는다.
            throw new EmailDomainException(EmailErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private void recordFailure(Exception error, long durationMs) {
        SmtpFailureDetails details = SmtpFailureDetails.from(error);
        log.warn("SMTP 이메일 발송 실패: {} {} {} {} {} {} {} {} {} {} {}",
            kv("event", "email_send_failed"), kv("provider", "SMTP"), kv("operation", "SEND_EMAIL"),
            kv("result", "FAILURE"), kv("errorClass", error.getClass().getSimpleName()),
            kv("errorReason", details.errorReason()), kv("causeErrorClass", details.causeErrorClass()),
            kv("failureStage", details.failureStage()), kv("smtpResponseCode", details.smtpResponseCode()),
            kv("smtpEnhancedStatusCode", details.smtpEnhancedStatusCode()), kv("durationMs", durationMs));

        Span span = tracer.currentSpan();
        if (span != null) {
            span.tag("email.provider", "SMTP");
            span.tag("email.error.reason", details.errorReason());
            span.tag("email.error.cause_class", details.causeErrorClass());
            span.tag("email.error.stage", details.failureStage());
            if (details.smtpResponseCode() != null) {
                span.tag("email.smtp.response_code", details.smtpResponseCode().toString());
            }
            if (details.smtpEnhancedStatusCode() != null) {
                span.tag("email.smtp.enhanced_status_code", details.smtpEnhancedStatusCode());
            }
        }
    }
}
