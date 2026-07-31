package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistorySearchQuery;
import com.umc.product.recruiting.domain.enums.RecruitingDecisionHistorySortOrder;
import com.umc.product.recruiting.domain.enums.RecruitingDecisionResult;

public record RecruitingDecisionHistorySearchGraphQlRequest(
    Long gisuId,
    List<Long> chapterIds,
    List<Long> schoolIds,
    List<ChallengerTrack> tracks,
    List<RecruitingDecisionResult> results,
    String searchName,
    RecruitingDecisionHistorySortOrder sort,
    Boolean groupByDecider,
    Integer page,
    Integer size
) {

    public RecruitingDecisionHistorySearchGraphQlRequest {
        requirePositive(gisuId, "gisuId");
        requirePositive(chapterIds, "chapterIds");
        requirePositive(schoolIds, "schoolIds");
    }

    public RecruitingDecisionHistorySearchQuery toQuery(Long requesterMemberId) {
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? 20 : size;
        if (pageNumber < 0 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("page는 0 이상, size는 1 이상 100 이하여야 합니다.");
        }
        return RecruitingDecisionHistorySearchQuery.builder()
            .gisuId(gisuId)
            .chapterIds(chapterIds == null ? Set.of() : Set.copyOf(chapterIds))
            .schoolIds(schoolIds == null ? Set.of() : Set.copyOf(schoolIds))
            .tracks(tracks == null ? Set.of() : Set.copyOf(tracks))
            .results(results == null ? Set.of() : Set.copyOf(results))
            .searchName(searchName)
            .sortOrder(sort)
            .groupByDecider(Boolean.TRUE.equals(groupByDecider))
            .requesterMemberId(requesterMemberId)
            .pageable(PageRequest.of(pageNumber, pageSize))
            .build();
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + "는 양수여야 합니다.");
        }
    }

    private static void requirePositive(List<Long> values, String fieldName) {
        if (values != null && values.stream().anyMatch(value -> value == null || value <= 0)) {
            throw new IllegalArgumentException(fieldName + "는 양수여야 합니다.");
        }
    }
}
