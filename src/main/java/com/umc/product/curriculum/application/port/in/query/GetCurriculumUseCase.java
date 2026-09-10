package com.umc.product.curriculum.application.port.in.query;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumOverviewInfo;
import com.umc.product.curriculum.application.port.in.query.dto.MyCurriculumInfo;

public interface GetCurriculumUseCase {

    /**
     * V2: 기수+파트 기반 커리큘럼 개요 조회 (WeeklyCurriculum 단위)
     *
     * @param weekNo null이면 전체 주차, 값이 있으면 해당 주차만 반환
     */
    CurriculumOverviewInfo getCurriculumOverview(Long gisuId, ChallengerPart part, Long weekNo);

    CurriculumOverviewInfo getCurriculumOverview(Long gisuId, ChallengerPart part, ChallengerTrack track, Long weekNo);

    /**
     * V2: 내 커리큘럼 진행 상황 조회 (WeeklyCurriculum → OriginalWorkbook → Mission 구조)
     */
    MyCurriculumInfo getMyProgress(Long memberId, Long gisuId);

    MyCurriculumInfo getMyProgress(Long memberId, Long gisuId, ChallengerTrack track);
}
