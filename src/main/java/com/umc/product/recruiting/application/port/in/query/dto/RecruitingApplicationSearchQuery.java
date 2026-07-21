package com.umc.product.recruiting.application.port.in.query.dto;

import org.springframework.data.domain.Pageable;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

import lombok.Builder;

@Builder
public record RecruitingApplicationSearchQuery(
    Long roundId,
    RecruitingApplicationStatus status,
    ChallengerTrack track,
    Long requesterMemberId,
    Pageable pageable
) {
}
