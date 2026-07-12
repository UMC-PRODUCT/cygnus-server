package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.recruiting.domain.RecruitingRoundEvaluator;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

public interface RecruitingRoundEvaluatorJpaRepository extends JpaRepository<RecruitingRoundEvaluator, Long> {

    Optional<RecruitingRoundEvaluator> findByRound_IdAndMemberIdAndStage(
        Long roundId,
        Long memberId,
        RecruitingEvaluatorStage stage
    );

    List<RecruitingRoundEvaluator> findAllByRound_IdAndStageOrderByMemberIdAscIdAsc(
        Long roundId,
        RecruitingEvaluatorStage stage
    );

    boolean existsByRound_IdAndMemberIdAndStage(
        Long roundId,
        Long memberId,
        RecruitingEvaluatorStage stage
    );
}
