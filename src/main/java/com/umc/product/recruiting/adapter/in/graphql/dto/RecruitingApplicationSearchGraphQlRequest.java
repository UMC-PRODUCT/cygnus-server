package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationSearchQuery;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public record RecruitingApplicationSearchGraphQlRequest(
    List<RecruitingApplicationStatus> statuses,
    List<ChallengerTrack> tracks
) {

    public RecruitingApplicationSearchQuery toQuery(Long roundId, Long requesterMemberId, Pageable pageable) {
        return RecruitingApplicationSearchQuery.builder()
            .roundId(roundId)
            .statuses(statuses == null ? Set.of() : Set.copyOf(statuses))
            .tracks(tracks == null ? Set.of() : Set.copyOf(tracks))
            .requesterMemberId(requesterMemberId)
            .pageable(pageable)
            .build();
    }
}
