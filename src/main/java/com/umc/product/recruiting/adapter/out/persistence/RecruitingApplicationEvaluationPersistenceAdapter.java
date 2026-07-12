package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationEvaluationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationEvaluationPort;
import com.umc.product.recruiting.domain.RecruitingApplicationEvaluation;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingApplicationEvaluationPersistenceAdapter
    implements LoadRecruitingApplicationEvaluationPort, SaveRecruitingApplicationEvaluationPort {

    private final RecruitingApplicationEvaluationJpaRepository repository;

    @Override
    public RecruitingApplicationEvaluation getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecruitingDomainException(
                RecruitingErrorCode.RECRUITING_APPLICATION_EVALUATION_NOT_FOUND
            ));
    }

    @Override
    public Optional<RecruitingApplicationEvaluation> findByApplicationIdAndEvaluatorMemberIdAndStage(
        Long applicationId,
        Long evaluatorMemberId,
        RecruitingEvaluatorStage stage
    ) {
        return repository.findByApplication_IdAndEvaluatorMemberIdAndStage(
            applicationId,
            evaluatorMemberId,
            stage
        );
    }

    @Override
    public List<RecruitingApplicationEvaluation> listByApplicationIdAndStage(
        Long applicationId,
        RecruitingEvaluatorStage stage
    ) {
        return repository.findAllByApplication_IdAndStageOrderByEvaluatorMemberIdAscIdAsc(applicationId, stage);
    }

    @Override
    public RecruitingApplicationEvaluation saveEvaluation(RecruitingApplicationEvaluation evaluation) {
        return repository.save(evaluation);
    }
}
