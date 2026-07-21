package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundGroupSearchQuery;
import com.umc.product.recruiting.domain.enums.RecruitingRoundSort;

public record RecruitingRoundSearchGraphQlRequest(
    Long gisuId,
    Long chapterId,
    Long schoolId,
    Long seasonId,
    ChallengerTrack track,
    RecruitingRoundSort sort
) {

    public RecruitingRoundSearchGraphQlRequest {
        requirePositive(gisuId, "gisuId");
        requirePositiveIfPresent(chapterId, "chapterId");
        requirePositiveIfPresent(schoolId, "schoolId");
        requirePositiveIfPresent(seasonId, "seasonId");
    }

    public RecruitingRoundGroupSearchQuery toQuery(Long requesterMemberId) {
        return RecruitingRoundGroupSearchQuery.builder()
            .gisuId(gisuId)
            .chapterId(chapterId)
            .schoolId(schoolId)
            .seasonId(seasonId)
            .track(track)
            .sort(sort)
            .requesterMemberId(requesterMemberId)
            .build();
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + "는 양수여야 합니다.");
        }
    }

    private static void requirePositiveIfPresent(Long value, String fieldName) {
        if (value != null) {
            requirePositive(value, fieldName);
        }
    }
}
