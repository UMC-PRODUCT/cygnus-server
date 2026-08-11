package com.umc.product.recruiting.adapter.in.graphql.dto;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundGroupSearchQuery;
import com.umc.product.recruiting.domain.enums.RecruitingRoundSort;

public record RecruitingRoundSearchGraphQlRequest(
    String gisuId,
    String chapterId,
    String schoolId,
    String seasonId,
    ChallengerTrack track,
    RecruitingRoundSort sort
) {

    public RecruitingRoundGroupSearchQuery toQuery(Long requesterMemberId) {
        return RecruitingRoundGroupSearchQuery.builder()
            .gisuId(requirePositive(GlobalId.decodeLong(gisuId, GlobalIdTypes.GISU), "gisuId"))
            .chapterId(decodePositiveIfPresent(chapterId, GlobalIdTypes.CHAPTER, "chapterId"))
            .schoolId(decodePositiveIfPresent(schoolId, GlobalIdTypes.SCHOOL, "schoolId"))
            .seasonId(decodePositiveIfPresent(seasonId, GlobalIdTypes.RECRUITING_SEASON, "seasonId"))
            .track(track)
            .sort(sort)
            .requesterMemberId(requesterMemberId)
            .build();
    }

    private static Long decodePositiveIfPresent(String value, String typeName, String fieldName) {
        if (value == null) {
            return null;
        }
        return requirePositive(GlobalId.decodeLong(value, typeName), fieldName);
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + "는 양수여야 합니다.");
        }
        return value;
    }
}
