package com.umc.product.notification.application.service;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.umc.product.notification.application.port.in.DeliverTemplateEmailUseCase;
import com.umc.product.notification.application.port.out.SendEmailPort;
import com.umc.product.notification.application.port.out.dto.EmailMessage;
import com.umc.product.notification.domain.TemplateEmailRequestedEvent;
import com.umc.product.notification.domain.exception.EmailDomainException;
import com.umc.product.notification.domain.exception.EmailErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TemplateEmailDispatchService implements DeliverTemplateEmailUseCase {

    private final TemplateEngine templateEngine;
    private final SendEmailPort sendEmailPort;
    private final EmailTemplateCatalog templateCatalog;
    private final EmailSenderProperties senderProperties;

    @Override
    public void deliver(TemplateEmailRequestedEvent event) {
        String htmlBody = render(event);
        EmailMessage message = new EmailMessage(
            senderProperties.noReplyAddress(),
            senderProperties.noReplyDisplayName(),
            event.recipient(),
            templateCatalog.subject(event.type()),
            htmlBody
        );
        try {
            sendEmailPort.send(message);
        } catch (EmailDomainException exception) {
            throw new EmailDomainException(
                (EmailErrorCode) exception.getBaseCode(),
                exception.retryable()
            );
        } catch (RuntimeException exception) {
            throw new EmailDomainException(EmailErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private String render(TemplateEmailRequestedEvent event) {
        try {
            Context context = new Context(Locale.KOREAN);
            event.variables().forEach(context::setVariable);
            return templateEngine.process(templateCatalog.templateResourcePath(event.type()), context);
        } catch (RuntimeException exception) {
            throw new EmailDomainException(EmailErrorCode.EMAIL_TEMPLATE_RENDER_FAILED);
        }
    }
}
