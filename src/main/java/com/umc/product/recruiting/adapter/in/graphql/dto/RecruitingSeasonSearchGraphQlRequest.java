package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonSearchQuery;

public record RecruitingSeasonSearchGraphQlRequest(
    Long gisuId,
    Long chapterId,
    Long schoolId
) {

    public RecruitingSeasonSearchGraphQlRequest {
        requirePositive(gisuId, "gisuId");
        requirePositiveIfPresent(chapterId, "chapterId");
        requirePositiveIfPresent(schoolId, "schoolId");
    }

    public RecruitingSeasonSearchQuery toQuery(Long requesterMemberId) {
        return RecruitingSeasonSearchQuery.builder()
            .gisuId(gisuId)
            .chapterId(chapterId)
            .schoolId(schoolId)
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
