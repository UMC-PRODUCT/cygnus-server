package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.recruiting.domain.RecruitingInterviewEvaluation;
import com.umc.product.recruiting.domain.enums.RecruitingInterviewEvaluationStatus;

public interface RecruitingInterviewEvaluationJpaRepository extends JpaRepository<RecruitingInterviewEvaluation, Long> {

    Optional<RecruitingInterviewEvaluation> findByApplication_IdAndEvaluatorMemberId(
        Long applicationId,
        Long evaluatorMemberId
    );

    List<RecruitingInterviewEvaluation> findAllByApplication_IdOrderByEvaluatorMemberIdAscIdAsc(Long applicationId);

    List<RecruitingInterviewEvaluation> findAllByApplication_IdAndStatusOrderByEvaluatorMemberIdAscIdAsc(
        Long applicationId,
        RecruitingInterviewEvaluationStatus status
    );

    boolean existsByApplication_IdAndEvaluatorMemberIdAndStatus(
        Long applicationId,
        Long evaluatorMemberId,
        RecruitingInterviewEvaluationStatus status
    );
}
