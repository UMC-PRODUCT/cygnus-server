package com.umc.product.recruiting.application.port.in.query.dto;

import java.time.Instant;

import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewScheduleStatus;
import com.umc.product.recruiting.domain.enums.RecruitingMailDeliveryStatus;

public record RecruitingInterviewScheduleInfo(
    Long id,
    Long applicationId,
    Long availabilityFormResponseId,
    RecruitingInterviewScheduleStatus status,
    Instant startsAt,
    Instant endsAt,
    String location,
    String contactSnapshot,
    RecruitingMailDeliveryStatus requestMailStatus,
    int requestMailAttempts,
    String requestMailError,
    Instant requestMailSentAt,
    RecruitingMailDeliveryStatus confirmationMailStatus,
    int confirmationMailAttempts,
    String confirmationMailError,
    Instant confirmationMailSentAt
) {

    public static RecruitingInterviewScheduleInfo from(RecruitingInterviewSchedule schedule) {
        return new RecruitingInterviewScheduleInfo(
            schedule.getId(),
            schedule.getApplication().getId(),
            schedule.getAvailabilityFormResponseId(),
            schedule.getStatus(),
            schedule.getStartsAt(),
            schedule.getEndsAt(),
            schedule.getLocation(),
            schedule.getContactSnapshot(),
            schedule.getRequestMailStatus(),
            schedule.getRequestMailAttempts(),
            schedule.getRequestMailError(),
            schedule.getRequestMailSentAt(),
            schedule.getConfirmationMailStatus(),
            schedule.getConfirmationMailAttempts(),
            schedule.getConfirmationMailError(),
            schedule.getConfirmationMailSentAt()
        );
    }
}
