package com.umc.product.recruiting.application.port.in.query;

import java.util.List;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationFormInfo;

public interface GetRecruitingFormQueryUseCase {

    List<RecruitingApplicationFormInfo> listPublicForms(Long gisuId, Long schoolId);

    boolean isApplicationFormBelongsToSeason(Long applicationFormId, Long seasonId);

    boolean isFormBelongsToSeason(Long formId, Long seasonId);
}
