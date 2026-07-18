package com.umc.product.recruiting.application.port.in.query.dto;

import com.umc.product.recruiting.domain.RecruitingInterviewSchedule;
import com.umc.product.recruiting.domain.enums.RecruitingMailDeliveryStatus;

import lombok.Builder;

@Builder
public record RecruitingInterviewRequestMailInfo(
    Long applicationId,
    String recipientEmail,
    String applicantName,
    Long availabilityFormId,
    String contactText,
    RecruitingMailDeliveryStatus deliveryStatus
) {

    public static RecruitingInterviewRequestMailInfo from(RecruitingInterviewSchedule schedule) {
        return RecruitingInterviewRequestMailInfo.builder()
            .applicationId(schedule.getApplication().getId())
            .recipientEmail(schedule.getApplication().getApplicantEmail())
            .applicantName(schedule.getApplication().getApplicantName())
            .availabilityFormId(schedule.getApplication().getRound().getAvailabilityFormId())
            .contactText(schedule.getContactSnapshot())
            .deliveryStatus(schedule.getRequestMailStatus())
            .build();
    }

    public boolean isSent() {
        return deliveryStatus == RecruitingMailDeliveryStatus.SENT;
    }
}
