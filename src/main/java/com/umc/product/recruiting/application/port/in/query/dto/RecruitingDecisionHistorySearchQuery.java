package com.umc.product.recruiting.application.port.in.query.dto;

import java.util.Set;

import org.springframework.data.domain.Pageable;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingDecisionHistorySortOrder;
import com.umc.product.recruiting.domain.enums.RecruitingDecisionResult;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

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

    private static final int MAX_PAGE_SIZE = 100;

    public RecruitingDecisionHistorySearchQuery {
        tracks = tracks == null ? Set.of() : Set.copyOf(tracks);
        results = results == null ? Set.of() : Set.copyOf(results);
        searchName = normalizeSearchName(searchName);
        validatePageSize(pageable);
    }

    private static void validatePageSize(Pageable pageable) {
        if (pageable != null && pageable.isPaged() && pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_DECISION_HISTORY_INVALID_PAGE_SIZE);
        }
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
