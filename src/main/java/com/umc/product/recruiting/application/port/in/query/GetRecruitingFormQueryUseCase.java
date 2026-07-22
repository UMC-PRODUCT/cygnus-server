package com.umc.product.recruiting.application.port.in.query;

import java.util.Optional;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationFormInfo;

public interface GetRecruitingFormQueryUseCase {

    FormWithStructureInfo getPublicFormStructure(
        Long applicationFormId,
        ChallengerTrack firstChoice,
        ChallengerTrack secondChoice
    );

    Optional<RecruitingApplicationFormInfo> findApplicationFormByRoundId(Long roundId);

    boolean isApplicationFormBelongsToSeason(Long applicationFormId, Long seasonId);

    boolean isFormBelongsToSeason(Long formId, Long seasonId);
}
