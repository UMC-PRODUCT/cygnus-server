package com.umc.product.notification.application.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.umc.product.notification.application.port.in.SendEmailUseCase;
import com.umc.product.notification.application.port.in.dto.SendHtmlEmailCommand;
import com.umc.product.notification.application.port.in.dto.SendVerificationEmailCommand;
import com.umc.product.notification.application.port.out.SendEmailPort;
import com.umc.product.notification.application.port.out.dto.EmailMessage;
import com.umc.product.notification.domain.exception.EmailDomainException;
import com.umc.product.notification.domain.exception.EmailErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendEmailService implements SendEmailUseCase {

    private static final String VERIFICATION_SUBJECT = "[UMC] 이메일 인증 코드";
    // 인증메일 수신 문제를 확인하기 위해 링크와 HTML 없이 발송한다. 다른 안내 메일의 HTML은 유지한다.
    private static final String VERIFICATION_BODY = """
        UMC 이메일 인증 안내

        인증 코드: %s

        이 코드는 요청 시점부터 10분 동안 유효합니다.
        직접 요청하지 않았다면 이 메일을 무시해 주세요.
        """;

    private final TemplateEngine templateEngine;
    private final SendEmailPort sendEmailPort;
    private final EmailSenderProperties senderProperties;

    @Async("emailTaskExecutor")
    @Override
    public void sendVerificationEmail(SendVerificationEmailCommand command) {
        // 인증 이메일은 no-reply 발신자로 고정. 향후 다른 case (예: support) 가 추가되면 분기한다.
        EmailMessage message = new EmailMessage(
            senderProperties.noReplyAddress(),
            senderProperties.noReplyDisplayName(),
            command.to(),
            VERIFICATION_SUBJECT,
            VERIFICATION_BODY.formatted(command.verificationCode()),
            false
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
    public void sendHtmlEmail(SendHtmlEmailCommand command) {
        String htmlContent = renderHtmlTemplate(command);
        EmailMessage message = new EmailMessage(
            senderProperties.noReplyAddress(),
            senderProperties.noReplyDisplayName(),
            command.to(),
            command.subject(),
            htmlContent,
            true
        );
        try {
            sendEmailPort.send(message);
        } catch (EmailDomainException e) {
            log.error("HTML 이메일 발송 실패: recipientPresent={}", hasRecipient(command.to()), e);
            throw e;
        }
    }

    private String renderHtmlTemplate(SendHtmlEmailCommand command) {
        try {
            Context context = new Context();
            context.setVariables(command.variables());
            return templateEngine.process(command.templateName(), context);
        } catch (RuntimeException e) {
            log.error("HTML 이메일 템플릿 렌더링 실패: recipientPresent={}", hasRecipient(command.to()), e);
            throw new EmailDomainException(EmailErrorCode.EMAIL_TEMPLATE_RENDER_FAILED, e);
        }
    }

    private boolean hasRecipient(String recipient) {
        return recipient != null && !recipient.isBlank();
    }
}
