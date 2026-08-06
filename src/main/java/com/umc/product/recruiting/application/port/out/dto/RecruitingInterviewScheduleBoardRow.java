package com.umc.product.recruiting.application.port.out.dto;

import java.time.Instant;

import com.umc.product.recruiting.domain.enums.RecruitingInterviewScheduleStatus;

public record RecruitingInterviewScheduleBoardRow(
    Long scheduleId,
    Long applicationId,
    String applicantName,
    Long availabilityFormResponseId,
    RecruitingInterviewScheduleStatus status,
    Long interviewSessionId,
    Instant startsAt
) {
}
