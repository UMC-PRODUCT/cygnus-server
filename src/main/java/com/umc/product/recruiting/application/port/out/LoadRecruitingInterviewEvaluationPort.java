package com.umc.product.recruiting.application.port.out;

import java.util.List;
import java.util.Optional;

import com.umc.product.recruiting.domain.RecruitingInterviewEvaluation;

public interface LoadRecruitingInterviewEvaluationPort {

    Optional<RecruitingInterviewEvaluation> findEvaluationById(Long id);

    RecruitingInterviewEvaluation getEvaluationById(Long id);

    Optional<RecruitingInterviewEvaluation> findByApplicationIdAndEvaluatorMemberId(
        Long applicationId,
        Long evaluatorMemberId
    );

    List<RecruitingInterviewEvaluation> listByApplicationId(Long applicationId);

    List<RecruitingInterviewEvaluation> listSubmittedByApplicationId(Long applicationId);

    boolean existsSubmittedByApplicationIdAndEvaluatorMemberId(Long applicationId, Long evaluatorMemberId);
}
