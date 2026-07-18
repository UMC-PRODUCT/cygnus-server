package com.umc.product.notification.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.event.application.port.out.dto.OutboxPublishResult;
import com.umc.product.notification.application.port.in.SendEmailUseCase;
import com.umc.product.notification.application.port.in.dto.SendTemplateEmailCommand;
import com.umc.product.notification.application.port.in.dto.SendVerificationEmailCommand;
import com.umc.product.notification.application.port.in.dto.TemplateEmailRequestInfo;
import com.umc.product.notification.application.port.out.SendEmailPort;
import com.umc.product.notification.application.port.out.dto.EmailMessage;
import com.umc.product.notification.domain.TemplateEmailRequestedEvent;
import com.umc.product.notification.domain.exception.EmailDomainException;
import com.umc.product.notification.domain.exception.EmailErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendEmailService implements SendEmailUseCase {

    private static final String VERIFICATION_TEMPLATE = "email/verification";
    private static final String SUBJECT_PREFIX = "이메일 인증 코드: ";

    private final TemplateEngine templateEngine;
    private final SendEmailPort sendEmailPort;
    private final EmailSenderProperties senderProperties;
    private final EmailTemplateCatalog templateCatalog;
    private final DomainEventPublisher domainEventPublisher;

    @Async("emailTaskExecutor")
    @Override
    public void sendVerificationEmail(SendVerificationEmailCommand command) {
        String htmlContent = renderVerificationTemplate(command);
        // 인증 이메일은 no-reply 발신자로 고정. 향후 다른 case (예: support) 가 추가되면 분기한다.
        EmailMessage message = new EmailMessage(
            senderProperties.noReplyAddress(),
            senderProperties.noReplyDisplayName(),
            command.to(),
            SUBJECT_PREFIX + command.verificationCode(),
            htmlContent
        );
        // 인증 이메일은 발송 실패 시 사용자가 가입/로그인을 진행할 수 없는 핵심 경로다.
        // 어댑터(SesEmailAdapter)의 WARN 과 별개로, 인증 usecase 에서는 ERROR 로 남겨 운영자가 즉시 인지하도록 한다.
        // 예외는 그대로 재던져 비동기 핸들러/상위 흐름이 처리하게 둔다.
        try {
            sendEmailPort.send(message);
        } catch (EmailDomainException e) {
            log.error("인증 이메일 발송 실패: recipientPresent={}", hasRecipient(command.to()), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public TemplateEmailRequestInfo requestTemplateEmail(SendTemplateEmailCommand command) {
        SendTemplateEmailCommand validated = templateCatalog.validate(command);
        TemplateEmailRequestedEvent event = new TemplateEmailRequestedEvent(
            validated.eventId(),
            Instant.now(),
            validated.recipient(),
            validated.templateType(),
            validated.variables()
        );
        OutboxPublishResult result = domainEventPublisher.publishOnce(event, validated.availableAt());
        Instant availableAt = validated.availableAt().truncatedTo(ChronoUnit.MICROS);
        return new TemplateEmailRequestInfo(
            result.eventId(),
            result.status(),
            result.deduplicated(),
            availableAt,
            result.nextAttemptAt()
        );
    }

    private String renderVerificationTemplate(SendVerificationEmailCommand command) {
        try {
            Context context = new Context();
            context.setVariable("verificationToken", command.verificationCode());
            return templateEngine.process(VERIFICATION_TEMPLATE, context);
        } catch (RuntimeException e) {
            // 예외 삼킴 방지: 비동기 컨텍스트에서도 원인 추적이 가능하도록 stacktrace 와 컨텍스트를 로그에 남긴다.
            log.error("이메일 템플릿 렌더링 실패: recipientPresent={}", hasRecipient(command.to()), e);
            throw new EmailDomainException(EmailErrorCode.EMAIL_TEMPLATE_RENDER_FAILED, e);
        }
    }

    private boolean hasRecipient(String recipient) {
        return recipient != null && !recipient.isBlank();
    }
}
