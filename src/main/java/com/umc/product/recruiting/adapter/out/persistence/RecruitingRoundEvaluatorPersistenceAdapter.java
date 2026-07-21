package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundEvaluatorPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingRoundEvaluatorPort;
import com.umc.product.recruiting.domain.RecruitingRoundEvaluator;
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
    public RecruitingRoundEvaluator getByRoundIdAndMemberId(Long roundId, Long memberId) {
        return repository.findByRound_IdAndMemberId(roundId, memberId)
            .orElseThrow(() -> new RecruitingDomainException(
                RecruitingErrorCode.RECRUITING_ROUND_EVALUATOR_NOT_FOUND
            ));
    }

    @Override
    public List<RecruitingRoundEvaluator> listByRoundId(Long roundId) {
        return repository.findAllByRound_IdOrderByMemberIdAscIdAsc(roundId);
    }

    @Override
    public boolean existsByRoundIdAndMemberId(Long roundId, Long memberId) {
        return repository.existsByRound_IdAndMemberId(roundId, memberId);
    }

    @Override
    public RecruitingRoundEvaluator save(RecruitingRoundEvaluator evaluator) {
        return repository.save(evaluator);
    }

    @Override
    public void delete(RecruitingRoundEvaluator evaluator) {
        repository.delete(evaluator);
    }

    @Override
    public void deleteByRoundId(Long roundId) {
        repository.deleteAllByRound_Id(roundId);
    }
}
