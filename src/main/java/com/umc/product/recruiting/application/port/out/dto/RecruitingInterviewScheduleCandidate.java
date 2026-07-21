package com.umc.product.recruiting.application.port.out.dto;

import java.time.Instant;

public record RecruitingInterviewScheduleCandidate(
    Instant startsAt,
    Instant endsAt,
    Integer availableApplicantCount
) {
}
