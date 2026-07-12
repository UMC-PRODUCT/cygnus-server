package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingRoundPort;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingRoundPersistenceAdapter implements LoadRecruitingRoundPort, SaveRecruitingRoundPort {

    private final RecruitingRoundJpaRepository recruitingRoundJpaRepository;

    @Override
    public Optional<RecruitingRound> findById(Long id) {
        return recruitingRoundJpaRepository.findById(id);
    }

    @Override
    public RecruitingRound getById(Long id) {
        return findById(id)
            .orElseThrow(() -> new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_NOT_FOUND));
    }

    @Override
    public RecruitingRound getByIdForUpdate(Long id) {
        return RecruitingLockExceptionTranslator.translate(() ->
            recruitingRoundJpaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_NOT_FOUND))
        );
    }

    @Override
    public List<RecruitingRound> listBySeasonId(Long seasonId) {
        return recruitingRoundJpaRepository.findAllBySeason_IdOrderByRoundNoAscIdAsc(seasonId);
    }

    @Override
    public boolean existsBySeasonIdAndTypeAndRoundNo(Long seasonId, RecruitingRoundType type, Integer roundNo) {
        return recruitingRoundJpaRepository.existsBySeason_IdAndTypeAndRoundNo(seasonId, type, roundNo);
    }

    @Override
    public RecruitingRound save(RecruitingRound round) {
        return recruitingRoundJpaRepository.save(round);
    }
}
