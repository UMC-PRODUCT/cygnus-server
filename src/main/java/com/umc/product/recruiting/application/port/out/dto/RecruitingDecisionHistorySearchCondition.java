package com.umc.product.recruiting.application.port.out.dto;

import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

import lombok.Builder;

/**
 * 판정 이력 검색 조건입니다.
 * <p>
 * {@code searchName}은 지원자 이름과 부분일치로 비교하며, 담당자 이름 검색은 서비스가 member 도메인에서
 * 이름이 일치하는 담당자를 먼저 찾아 {@code matchedDeciderMemberIds}로 전달합니다. 두 조건은 OR로 결합합니다.
 */
@Builder
public record RecruitingDecisionHistorySearchCondition(
    Long gisuId,
    Set<Long> schoolIds,
    Set<ChallengerTrack> tracks,
    Set<RecruitingApplicationStatus> decisionStatuses,
    String searchName,
    Set<Long> matchedDeciderMemberIds,
    boolean latestFirst,
    boolean groupByDecider
) {

    public RecruitingDecisionHistorySearchCondition {
        schoolIds = schoolIds == null ? Set.of() : Set.copyOf(schoolIds);
        tracks = tracks == null ? Set.of() : Set.copyOf(tracks);
        decisionStatuses = decisionStatuses == null ? Set.of() : Set.copyOf(decisionStatuses);
        matchedDeciderMemberIds = matchedDeciderMemberIds == null ? Set.of() : Set.copyOf(matchedDeciderMemberIds);
    }
}
