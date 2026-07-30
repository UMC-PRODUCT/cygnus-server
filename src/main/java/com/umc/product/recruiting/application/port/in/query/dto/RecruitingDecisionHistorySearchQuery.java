package com.umc.product.recruiting.application.port.in.query.dto;

import java.util.Set;

import org.springframework.data.domain.Pageable;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingDecisionHistorySortOrder;
import com.umc.product.recruiting.domain.enums.RecruitingDecisionResult;

import lombok.Builder;

@Builder
public record RecruitingDecisionHistorySearchQuery(
    Long gisuId,
    Long chapterId,
    Long schoolId,
    Set<ChallengerTrack> tracks,
    Set<RecruitingDecisionResult> results,
    String searchName,
    RecruitingDecisionHistorySortOrder sortOrder,
    boolean groupByDecider,
    Long requesterMemberId,
    Pageable pageable
) {

    public RecruitingDecisionHistorySearchQuery {
        tracks = tracks == null ? Set.of() : Set.copyOf(tracks);
        results = results == null ? Set.of() : Set.copyOf(results);
        searchName = normalizeSearchName(searchName);
    }

    public RecruitingDecisionHistorySortOrder effectiveSortOrder() {
        return sortOrder == null ? RecruitingDecisionHistorySortOrder.LATEST : sortOrder;
    }

    private static String normalizeSearchName(String searchName) {
        if (searchName == null || searchName.isBlank()) {
            return null;
        }
        return searchName.trim();
    }
}
