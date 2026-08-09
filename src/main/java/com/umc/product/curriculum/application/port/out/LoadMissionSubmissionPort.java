package com.umc.product.curriculum.application.port.out;

import java.util.List;

import com.umc.product.curriculum.domain.MissionSubmission;

public interface LoadMissionSubmissionPort {

    MissionSubmission getById(Long missionSubmissionId);

    MissionSubmission getByIdForUpdate(Long missionSubmissionId);

    /**
     * 챌린저 워크북에 속한 모든 미션 제출물 조회
     * <p>
     * (original_workbook_mission_id, challenger_workbook_id) UNIQUE 제약으로 미션 당 최대 1건
     */
    List<MissionSubmission> listActiveByChallengerWorkbookId(Long challengerWorkbookId);

    /**
     * 여러 챌린저 워크북에 속한 제출물 목록 일괄 조회 (N+1 방지)
     */
    List<MissionSubmission> listActiveByChallengerWorkbookIdIn(List<Long> challengerWorkbookIds);

    /**
     * 해당 원본 워크북 미션에 제출물이 하나라도 존재하는지 확인
     */
    boolean existsByOriginalWorkbookMissionId(Long originalWorkbookMissionId);

    boolean existsByOriginalWorkbookMissionIdAndChallengerWorkbookId(
        Long originalWorkbookMissionId,
        Long challengerWorkbookId
    );

    boolean existsByChallengerWorkbookId(Long challengerWorkbookId);
}
