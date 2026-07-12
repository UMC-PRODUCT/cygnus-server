package com.umc.product.recruiting.application.port.out;

import java.util.List;
import java.util.Optional;

import com.umc.product.recruiting.domain.RecruitingApplicationEvaluation;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

public interface LoadRecruitingApplicationEvaluationPort {

    RecruitingApplicationEvaluation getById(Long id);

    Optional<RecruitingApplicationEvaluation> findByApplicationIdAndEvaluatorMemberIdAndStage(
        Long applicationId,
        Long evaluatorMemberId,
        RecruitingEvaluatorStage stage
    );

    List<RecruitingApplicationEvaluation> listByApplicationIdAndStage(
        Long applicationId,
        RecruitingEvaluatorStage stage
    );
}
