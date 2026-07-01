package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LoadRecruitingInterviewEvaluationPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingInterviewEvaluationPort;
import com.umc.product.recruiting.domain.RecruitingInterviewEvaluation;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewEvaluationStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingInterviewEvaluationPersistenceAdapter
    implements LoadRecruitingInterviewEvaluationPort, SaveRecruitingInterviewEvaluationPort {

    private final RecruitingInterviewEvaluationJpaRepository recruitingInterviewEvaluationJpaRepository;

    @Override
    public Optional<RecruitingInterviewEvaluation> findEvaluationById(Long id) {
        return recruitingInterviewEvaluationJpaRepository.findById(id);
    }

    @Override
    public RecruitingInterviewEvaluation getEvaluationById(Long id) {
        return findEvaluationById(id)
            .orElseThrow(() -> new RecruitingDomainException(
                RecruitingErrorCode.RECRUITING_INTERVIEW_EVALUATION_NOT_FOUND));
    }

    @Override
    public Optional<RecruitingInterviewEvaluation> findByApplicationIdAndEvaluatorMemberId(
        Long applicationId,
        Long evaluatorMemberId
    ) {
        return recruitingInterviewEvaluationJpaRepository.findByApplication_IdAndEvaluatorMemberId(
            applicationId,
            evaluatorMemberId
        );
    }

    @Override
    public List<RecruitingInterviewEvaluation> listByApplicationId(Long applicationId) {
        return recruitingInterviewEvaluationJpaRepository
            .findAllByApplication_IdOrderByEvaluatorMemberIdAscIdAsc(applicationId);
    }

    @Override
    public List<RecruitingInterviewEvaluation> listSubmittedByApplicationId(Long applicationId) {
        return recruitingInterviewEvaluationJpaRepository.findAllByApplication_IdAndStatusOrderByEvaluatorMemberIdAscIdAsc(
            applicationId,
            RecruitingInterviewEvaluationStatus.SUBMITTED
        );
    }

    @Override
    public boolean existsSubmittedByApplicationIdAndEvaluatorMemberId(Long applicationId, Long evaluatorMemberId) {
        return recruitingInterviewEvaluationJpaRepository.existsByApplication_IdAndEvaluatorMemberIdAndStatus(
            applicationId,
            evaluatorMemberId,
            RecruitingInterviewEvaluationStatus.SUBMITTED
        );
    }

    @Override
    public RecruitingInterviewEvaluation saveEvaluation(RecruitingInterviewEvaluation evaluation) {
        return recruitingInterviewEvaluationJpaRepository.save(evaluation);
    }
}
