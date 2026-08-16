package com.umc.product.curriculum.application.port.out;

import java.util.List;

import com.umc.product.curriculum.domain.OriginalWorkbookMission;

public interface LoadOriginalWorkbookMissionPort {

    /**
     * 미션 단건 조회. 존재하지 않으면 CurriculumDomainException(MISSION_NOT_FOUND) 던짐
     */
    OriginalWorkbookMission getById(Long originalWorkbookMissionId);

    List<OriginalWorkbookMission> findByOriginalWorkbookId(Long originalWorkbookId);

    /**
     * 여러 원본 워크북에 속한 미션 목록 일괄 조회 (N+1 방지)
     */
    List<OriginalWorkbookMission> findByOriginalWorkbookIdIn(List<Long> originalWorkbookIds);
}
