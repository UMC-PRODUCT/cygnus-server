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
        return recruitingRoundJpaRepository.findActiveById(id);
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
    public RecruitingRound getByIdForUpdateIncludingDeleted(Long id) {
        return RecruitingLockExceptionTranslator.translate(() ->
            recruitingRoundJpaRepository.findByIdForUpdateIncludingDeleted(id)
                .orElseThrow(() -> new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_NOT_FOUND))
        );
    }

    @Override
    public List<RecruitingRound> listBySeasonId(Long seasonId) {
        return recruitingRoundJpaRepository.findAllBySeason_IdAndDeletedAtIsNullOrderByRoundNoAscIdAsc(seasonId);
    }

    @Override
    public List<RecruitingRound> listBySeasonIds(List<Long> seasonIds) {
        if (seasonIds.isEmpty()) {
            return List.of();
        }
        return recruitingRoundJpaRepository.findAllBySeasonIds(seasonIds);
    }

    @Override
    public boolean existsBySeasonIdAndTypeAndRoundNo(Long seasonId, RecruitingRoundType type, Integer roundNo) {
        return recruitingRoundJpaRepository
            .existsBySeason_IdAndTypeAndRoundNoAndDeletedAtIsNull(seasonId, type, roundNo);
    }

    @Override
    public boolean existsBySeasonIdAndTitleIgnoreCase(Long seasonId, String title) {
        return recruitingRoundJpaRepository.existsBySeason_IdAndTitleIgnoreCaseAndDeletedAtIsNull(seasonId, title);
    }

    @Override
    public boolean existsBySeasonIdAndTitleIgnoreCaseAndIdNot(Long seasonId, String title, Long id) {
        return recruitingRoundJpaRepository
            .existsBySeason_IdAndTitleIgnoreCaseAndIdNotAndDeletedAtIsNull(seasonId, title, id);
    }

    @Override
    public int getMaxAdditionalRoundNo(Long seasonId) {
        return recruitingRoundJpaRepository.findMaxAdditionalRoundNo(seasonId);
    }

    @Override
    public RecruitingRound save(RecruitingRound round) {
        return recruitingRoundJpaRepository.save(round);
    }
}
