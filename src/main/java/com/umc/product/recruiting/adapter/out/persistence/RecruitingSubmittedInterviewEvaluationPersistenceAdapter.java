package com.umc.product.recruiting.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LoadRecruitingSubmittedInterviewEvaluationPort;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingSubmittedInterviewEvaluationPersistenceAdapter
    implements LoadRecruitingSubmittedInterviewEvaluationPort {

    private final RecruitingApplicationEvaluationJpaRepository repository;

    @Override
    public boolean existsSubmittedByRoundId(Long roundId) {
        return repository.existsByApplication_Round_IdAndStage(
            roundId,
            RecruitingEvaluatorStage.INTERVIEW
        );
    }

    @Override
    public boolean existsSubmittedByApplicationId(Long applicationId) {
        return repository.existsByApplication_IdAndStage(
            applicationId,
            RecruitingEvaluatorStage.INTERVIEW
        );
    }
}
