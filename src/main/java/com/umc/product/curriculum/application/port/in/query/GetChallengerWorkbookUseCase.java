package com.umc.product.curriculum.application.port.in.query;

import com.umc.product.curriculum.application.port.in.query.dto.ChallengerWorkbookInfo;

/**
 * ChallengerWorkbook 조회 UseCase
 */
public interface GetChallengerWorkbookUseCase {

    // <----------------------------- 챌린저 기능 ----------------------------->

    /**
     * 챌린저 워크북 상세 조회
     * <p>
     * 같은 기수에 활동한 모든 챌린저가 조회 가능합니다.
     * 미션 제출 내역 및 피드백을 포함합니다.
     *
     * @param challengerWorkbookId 챌린저 워크북 ID
     * @return 챌린저 워크북 상세 정보 (미션 제출물 및 피드백 포함)
     */
    ChallengerWorkbookInfo getById(Long challengerWorkbookId, Long requesterMemberId);

}
