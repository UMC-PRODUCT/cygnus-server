package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Pageable;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistorySearchQuery;
import com.umc.product.recruiting.domain.enums.RecruitingDecisionHistorySortOrder;
import com.umc.product.recruiting.domain.enums.RecruitingDecisionResult;

public record RecruitingDecisionHistorySearchGraphQlRequest(
    String gisuId,
    List<String> chapterIds,
    List<String> schoolIds,
    List<ChallengerTrack> tracks,
    List<RecruitingDecisionResult> results,
    String searchName,
    RecruitingDecisionHistorySortOrder sort,
    Boolean groupByDecider
) {

    public RecruitingDecisionHistorySearchQuery toQuery(Long requesterMemberId, Pageable pageable) {
        return RecruitingDecisionHistorySearchQuery.builder()
            .gisuId(requirePositive(GlobalId.decodeLong(gisuId, GlobalIdTypes.GISU), "gisuId"))
            .chapterIds(decodePositiveIds(chapterIds, GlobalIdTypes.CHAPTER, "chapterIds"))
            .schoolIds(decodePositiveIds(schoolIds, GlobalIdTypes.SCHOOL, "schoolIds"))
            .tracks(tracks == null ? Set.of() : Set.copyOf(tracks))
            .results(results == null ? Set.of() : Set.copyOf(results))
            .searchName(searchName)
            .sortOrder(sort)
            .groupByDecider(Boolean.TRUE.equals(groupByDecider))
            .requesterMemberId(requesterMemberId)
            .pageable(pageable)
            .build();
    }

    private static Set<Long> decodePositiveIds(List<String> values, String typeName, String fieldName) {
        if (values == null) {
            return Set.of();
        }
        if (values.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "는 하나 이상이어야 합니다.");
        }
        Set<Long> decoded = Set.copyOf(GlobalId.decodeLongs(values, typeName));
        if (decoded.stream().anyMatch(value -> value <= 0)) {
            throw new IllegalArgumentException(fieldName + "는 양수여야 합니다.");
        }
        return decoded;
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + "는 양수여야 합니다.");
        }
        return value;
    }
}
