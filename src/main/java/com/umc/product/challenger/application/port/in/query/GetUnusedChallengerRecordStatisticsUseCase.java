package com.umc.product.challenger.application.port.in.query;

import java.util.List;

import com.umc.product.challenger.application.port.in.query.dto.UnusedChallengerRecordCountInfo;

public interface GetUnusedChallengerRecordStatisticsUseCase {

    /**
     * 기수×학교 단위로 그룹 집계한 미사용(isUsed=false) 챌린저 기록 코드 개수를 반환합니다.
     * <p>
     * 미사용 코드가 0개인 (기수, 학교) 조합은 결과에 포함되지 않으며,
     * 기수 내림차순(최신 우선) · 학교 오름차순으로 정렬됩니다.
     */
    List<UnusedChallengerRecordCountInfo> getUnusedCountByGisuAndSchool();
}
