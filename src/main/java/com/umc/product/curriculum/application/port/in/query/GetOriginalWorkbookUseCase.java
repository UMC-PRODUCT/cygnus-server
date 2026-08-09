package com.umc.product.curriculum.application.port.in.query;

import com.umc.product.curriculum.application.port.in.query.dto.OriginalWorkbookInfo;

/**
 * OriginalWorkbook 조회 UseCase
 */
public interface GetOriginalWorkbookUseCase {

    /**
     * 원본 워크북 상세 조회
     * <p>
     * 원본 워크북의 파트와 기수에 해당 파트의 스터디 그룹에 속해 있어야 합니다.
     *
     * @param originalWorkbookId 원본 워크북 ID
     * @return 원본 워크북 상세 정보 (미션 포함)
     */
    OriginalWorkbookInfo getById(Long originalWorkbookId, Long requesterMemberId);

}
