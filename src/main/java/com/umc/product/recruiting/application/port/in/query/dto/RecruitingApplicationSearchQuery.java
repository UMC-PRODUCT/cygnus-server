package com.umc.product.recruiting.application.port.in.query.dto;

import java.util.Set;

import org.springframework.data.domain.Pageable;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

import lombok.Builder;

@Builder
public record RecruitingApplicationSearchQuery(
    Long roundId,
    Set<RecruitingApplicationStatus> statuses,
    Set<ChallengerTrack> tracks,
    Long requesterMemberId,
    Pageable pageable
) {

    public RecruitingApplicationSearchQuery {
        statuses = statuses == null ? Set.of() : Set.copyOf(statuses);
        tracks = tracks == null ? Set.of() : Set.copyOf(tracks);
    }
}
