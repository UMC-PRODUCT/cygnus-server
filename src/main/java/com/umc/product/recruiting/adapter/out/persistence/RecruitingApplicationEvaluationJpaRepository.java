package com.umc.product.recruiting.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.recruiting.domain.RecruitingApplicationEvaluation;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

public interface RecruitingApplicationEvaluationJpaRepository
    extends JpaRepository<RecruitingApplicationEvaluation, Long> {

    Optional<RecruitingApplicationEvaluation> findByApplication_IdAndEvaluatorMemberIdAndStage(
        Long applicationId,
        Long evaluatorMemberId,
        RecruitingEvaluatorStage stage
    );

    List<RecruitingApplicationEvaluation> findAllByApplication_IdAndStageOrderByEvaluatorMemberIdAscIdAsc(
        Long applicationId,
        RecruitingEvaluatorStage stage
    );

    boolean existsByApplication_Round_IdAndStage(
        Long roundId,
        RecruitingEvaluatorStage stage
    );

    boolean existsByApplication_IdAndStage(
        Long applicationId,
        RecruitingEvaluatorStage stage
    );

    List<RecruitingApplicationEvaluation> findAllByApplication_IdInAndEvaluatorMemberId(
        Collection<Long> applicationIds,
        Long evaluatorMemberId
    );
}
