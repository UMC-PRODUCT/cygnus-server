package com.umc.product.recruiting.application.port.in.query.dto;

import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingRoundPhase;
import com.umc.product.recruiting.domain.enums.RecruitingRoundSort;

import lombok.Builder;

@Builder
public record RecruitingPublicRoundSearchQuery(
    Long gisuId,
    Long chapterId,
    Set<Long> schoolIds,
    Set<Long> roundIds,
    String schoolName,
    Long seasonId,
    ChallengerTrack track,
    RecruitingRoundPhase phase,
    RecruitingRoundSort sort
) {

    public RecruitingPublicRoundSearchQuery {
        schoolIds = schoolIds == null ? Set.of() : Set.copyOf(schoolIds);
        roundIds = roundIds == null ? Set.of() : Set.copyOf(roundIds);
        schoolName = normalizeSchoolName(schoolName);
    }

    public RecruitingRoundPhase effectivePhase() {
        return phase == null ? RecruitingRoundPhase.OPEN : phase;
    }

    public RecruitingRoundSort effectiveSort() {
        return sort == null ? RecruitingRoundSort.NEWEST : sort;
    }

    private static String normalizeSchoolName(String schoolName) {
        if (schoolName == null || schoolName.isBlank()) {
            return null;
        }
        return schoolName.trim();
    }
}
