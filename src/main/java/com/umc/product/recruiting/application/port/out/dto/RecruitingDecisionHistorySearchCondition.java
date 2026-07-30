package com.umc.product.recruiting.application.port.out.dto;

import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

import lombok.Builder;

/**
 * 판정 이력 검색 조건입니다.
 * <p>
 * {@code searchName}은 지원자 이름 또는 판정 시점 담당자 이름·닉네임과 부분일치로 비교합니다.
 */
@Builder
public record RecruitingDecisionHistorySearchCondition(
    Long gisuId,
    Set<Long> schoolIds,
    Set<ChallengerTrack> tracks,
    Set<RecruitingApplicationStatus> decisionStatuses,
    String searchName,
    boolean latestFirst,
    boolean groupByDecider
) {

    public RecruitingDecisionHistorySearchCondition {
        schoolIds = schoolIds == null ? Set.of() : Set.copyOf(schoolIds);
        tracks = tracks == null ? Set.of() : Set.copyOf(tracks);
        decisionStatuses = decisionStatuses == null ? Set.of() : Set.copyOf(decisionStatuses);
    }
}
