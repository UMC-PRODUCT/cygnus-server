package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundSearchQuery;

public record RecruitingRoundSearchGraphQlRequest(
    Long gisuId,
    Long chapterId,
    Long schoolId,
    Long seasonId
) {

    public RecruitingRoundSearchGraphQlRequest {
        requirePositive(gisuId, "gisuId");
        requirePositiveIfPresent(chapterId, "chapterId");
        requirePositiveIfPresent(schoolId, "schoolId");
        requirePositiveIfPresent(seasonId, "seasonId");
    }

    public RecruitingRoundSearchQuery toQuery(Long requesterMemberId) {
        return RecruitingRoundSearchQuery.builder()
            .gisuId(gisuId)
            .chapterId(chapterId)
            .schoolId(schoolId)
            .seasonId(seasonId)
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
