package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundEvaluatorPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingRoundEvaluatorPort;
import com.umc.product.recruiting.domain.RecruitingRoundEvaluator;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingRoundEvaluatorPersistenceAdapter implements
    LoadRecruitingRoundEvaluatorPort,
    SaveRecruitingRoundEvaluatorPort {

    private final RecruitingRoundEvaluatorJpaRepository repository;

    @Override
    public RecruitingRoundEvaluator getByRoundIdAndMemberIdAndStage(
        Long roundId,
        Long memberId,
        RecruitingEvaluatorStage stage
    ) {
        return repository.findByRound_IdAndMemberIdAndStage(roundId, memberId, stage)
            .orElseThrow(() -> new RecruitingDomainException(
                RecruitingErrorCode.RECRUITING_ROUND_EVALUATOR_NOT_FOUND
            ));
    }

    @Override
    public List<RecruitingRoundEvaluator> listByRoundIdAndStage(
        Long roundId,
        RecruitingEvaluatorStage stage
    ) {
        return repository.findAllByRound_IdAndStageOrderByMemberIdAscIdAsc(roundId, stage);
    }

    @Override
    public boolean existsByRoundIdAndMemberIdAndStage(
        Long roundId,
        Long memberId,
        RecruitingEvaluatorStage stage
    ) {
        return repository.existsByRound_IdAndMemberIdAndStage(roundId, memberId, stage);
    }

    @Override
    public RecruitingRoundEvaluator save(RecruitingRoundEvaluator evaluator) {
        return repository.save(evaluator);
    }

    @Override
    public void delete(RecruitingRoundEvaluator evaluator) {
        repository.delete(evaluator);
    }
}
