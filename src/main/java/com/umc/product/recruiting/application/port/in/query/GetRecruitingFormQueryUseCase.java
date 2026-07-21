package com.umc.product.recruiting.application.port.in.query;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;

public interface GetRecruitingFormQueryUseCase {

    FormWithStructureInfo getPublicFormStructure(
        Long applicationFormId,
        ChallengerTrack firstChoice,
        ChallengerTrack secondChoice
    );

    boolean isApplicationFormBelongsToSeason(Long applicationFormId, Long seasonId);

    boolean isFormBelongsToSeason(Long formId, Long seasonId);
}
