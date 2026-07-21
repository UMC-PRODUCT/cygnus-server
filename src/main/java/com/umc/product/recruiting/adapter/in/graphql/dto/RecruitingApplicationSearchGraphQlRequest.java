package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationSearchQuery;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public record RecruitingApplicationSearchGraphQlRequest(
    List<RecruitingApplicationStatus> statuses,
    List<ChallengerTrack> tracks,
    Integer page,
    Integer size
) {

    public RecruitingApplicationSearchQuery toQuery(Long roundId, Long requesterMemberId) {
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? 20 : size;
        if (pageNumber < 0 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("page는 0 이상, size는 1 이상 100 이하여야 합니다.");
        }
        return RecruitingApplicationSearchQuery.builder()
            .roundId(roundId)
            .statuses(statuses == null ? Set.of() : Set.copyOf(statuses))
            .tracks(tracks == null ? Set.of() : Set.copyOf(tracks))
            .requesterMemberId(requesterMemberId)
            .pageable(PageRequest.of(pageNumber, pageSize))
            .build();
    }
}
