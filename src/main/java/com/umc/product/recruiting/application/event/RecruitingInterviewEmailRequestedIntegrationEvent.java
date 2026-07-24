package com.umc.product.recruiting.application.event;

import java.time.Instant;
import java.util.UUID;

import com.umc.product.global.event.domain.IntegrationEvent;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingInterviewRequestMailInfo;

public record RecruitingInterviewEmailRequestedIntegrationEvent(
    UUID eventId,
    Instant occurredAt,
    UUID requestId,
    Detail detail
) implements IntegrationEvent {

    private static final String EVENT_TYPE = "recruiting.interview.email.requested.v1";
    private static final String SOURCE = "umc-product.recruiting";

    public RecruitingInterviewEmailRequestedIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        if (requestId == null || detail == null) {
            throw new IllegalArgumentException("채용 메일 integration event 정보가 올바르지 않습니다.");
        }
    }

    public static RecruitingInterviewEmailRequestedIntegrationEvent from(
        InterviewAvailabilityRequestedEvent source,
        RecruitingInterviewRequestMailInfo mail
    ) {
        return new RecruitingInterviewEmailRequestedIntegrationEvent(
            null,
            source.occurredAt(),
            source.eventId(),
            new Detail(
                mail.applicationId(),
                mail.recipientEmail(),
                mail.applicantName(),
                mail.availabilityFormId(),
                mail.contactText()
            )
        );
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    @Override
    public String source() {
        return SOURCE;
    }

    public record Detail(
        Long applicationId,
        String email,
        String applicantName,
        Long availabilityFormId,
        String contactText
    ) {
    }
}
