package com.umc.product.recruiting.application.port.in.query;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingAdminFormStructureInfo;

public interface GetRecruitingFormQueryUseCase {

    FormWithStructureInfo getPublicFormStructure(
        Long applicationFormId,
        ChallengerTrack firstChoice,
        ChallengerTrack secondChoice
    );

    /**
     * 운영진 편집기용 지원 Form 전체 구조를 조회한다.
     * 공개 조회와 달리 Form 상태와 지망 트랙에 관계없이 모든 section을 반환한다.
     */
    RecruitingAdminFormStructureInfo getAdminFormStructure(Long seasonId, Long roundId);

    boolean isApplicationFormBelongsToSeason(Long applicationFormId, Long seasonId);

    boolean isFormBelongsToSeason(Long formId, Long seasonId);
}
