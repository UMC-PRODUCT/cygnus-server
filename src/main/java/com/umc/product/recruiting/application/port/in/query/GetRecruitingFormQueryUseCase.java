package com.umc.product.recruiting.application.port.in.query;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationFormInfo;

public interface GetRecruitingFormQueryUseCase {

    List<RecruitingApplicationFormInfo> listPublicForms(Long gisuId, Long schoolId);

    FormWithStructureInfo getPublicFormStructure(
        Long applicationFormId,
        ChallengerTrack firstChoice,
        ChallengerTrack secondChoice
    );

    boolean isApplicationFormBelongsToSeason(Long applicationFormId, Long seasonId);

    boolean isFormBelongsToSeason(Long formId, Long seasonId);
}
