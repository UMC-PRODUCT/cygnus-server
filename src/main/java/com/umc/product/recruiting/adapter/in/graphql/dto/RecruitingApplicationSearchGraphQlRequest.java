package com.umc.product.recruiting.adapter.in.graphql.dto;

import org.springframework.data.domain.PageRequest;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationSearchQuery;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

public record RecruitingApplicationSearchGraphQlRequest(
    RecruitingApplicationStatus status,
    ChallengerTrack track,
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
            .status(status)
            .track(track)
            .requesterMemberId(requesterMemberId)
            .pageable(PageRequest.of(pageNumber, pageSize))
            .build();
    }
}
