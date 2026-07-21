package com.umc.product.recruiting.application.port.in.query;

import java.util.List;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationEvaluationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

public interface GetRecruitingApplicationEvaluationUseCase {

    List<RecruitingApplicationEvaluationInfo> listVisibleEvaluations(
        Long applicationId,
        Long requesterMemberId,
        RecruitingEvaluatorStage stage
    );
}
