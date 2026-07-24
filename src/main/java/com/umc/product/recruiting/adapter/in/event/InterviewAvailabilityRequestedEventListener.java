package com.umc.product.recruiting.adapter.in.event;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.umc.product.global.config.notification.NotificationTransportProperties;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.observability.ObservabilityErrorSanitizer;
import com.umc.product.notification.application.port.in.SendEmailUseCase;
import com.umc.product.notification.application.port.in.dto.SendHtmlEmailCommand;
import com.umc.product.recruiting.application.event.InterviewAvailabilityRequestedEvent;
import com.umc.product.recruiting.application.event.RecruitingInterviewEmailRequestedIntegrationEvent;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewMailDeliveryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewMailDeliveryUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewRequestMailInfo;

@Component
public class InterviewAvailabilityRequestedEventListener {

    private static final int MAX_ERROR_LENGTH = 2000;
    private static final String TEMPLATE_NAME = "email/recruiting-interview-availability";
    private static final String SUBJECT = "UMC 면접 가능 일정 제출 안내";

    private final SendEmailUseCase sendEmailUseCase;
    private final GetRecruitingInterviewMailDeliveryUseCase getMailDeliveryUseCase;
    private final ManageRecruitingInterviewMailDeliveryUseCase manageMailDeliveryUseCase;
    private final Clock clock;
    private final DomainEventPublisher eventPublisher;
    private final NotificationTransportProperties transportProperties;

    @Autowired
    public InterviewAvailabilityRequestedEventListener(
        SendEmailUseCase sendEmailUseCase,
        GetRecruitingInterviewMailDeliveryUseCase getMailDeliveryUseCase,
        ManageRecruitingInterviewMailDeliveryUseCase manageMailDeliveryUseCase,
        Clock clock,
        DomainEventPublisher eventPublisher,
        NotificationTransportProperties transportProperties
    ) {
        this.sendEmailUseCase = sendEmailUseCase;
        this.getMailDeliveryUseCase = getMailDeliveryUseCase;
        this.manageMailDeliveryUseCase = manageMailDeliveryUseCase;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
        this.transportProperties = transportProperties;
    }

    InterviewAvailabilityRequestedEventListener(
        SendEmailUseCase sendEmailUseCase,
        GetRecruitingInterviewMailDeliveryUseCase getMailDeliveryUseCase,
        ManageRecruitingInterviewMailDeliveryUseCase manageMailDeliveryUseCase,
        Clock clock
    ) {
        this(
            sendEmailUseCase,
            getMailDeliveryUseCase,
            manageMailDeliveryUseCase,
            clock,
            null,
            NotificationTransportProperties.local()
        );
    }

    @EventListener
    public void handle(InterviewAvailabilityRequestedEvent event) {
        RecruitingInterviewRequestMailInfo mail = getMailDeliveryUseCase.getRequestMail(event.applicationId());
        if (mail.isSent() || mail.isCancelled()) {
            return;
        }
        if (transportProperties.transport().sendsExternally()) {
            eventPublisher.publish(RecruitingInterviewEmailRequestedIntegrationEvent.from(event, mail));
        }
        if (!transportProperties.transport().sendsLocally()) {
            return;
        }
        try {
            sendEmailUseCase.sendHtmlEmail(command(mail));
        } catch (RuntimeException exception) {
            manageMailDeliveryUseCase.markRequestMailFailed(
                event.applicationId(),
                failureMessage(exception)
            );
            throw exception;
        }
        manageMailDeliveryUseCase.markRequestMailSent(event.applicationId(), Instant.now(clock));
    }

    private SendHtmlEmailCommand command(RecruitingInterviewRequestMailInfo mail) {
        return new SendHtmlEmailCommand(
            mail.recipientEmail(),
            SUBJECT,
            TEMPLATE_NAME,
            Map.of(
                "applicantName", mail.applicantName(),
                "availabilityFormId", mail.availabilityFormId(),
                "contactText", mail.contactText()
            )
        );
    }

    private String failureMessage(RuntimeException exception) {
        String message = ObservabilityErrorSanitizer.sanitizeMessage(exception.getMessage());
        String normalized = message == null || message.isBlank()
            ? exception.getClass().getName()
            : message;
        return normalized.substring(0, Math.min(normalized.length(), MAX_ERROR_LENGTH));
    }
}
