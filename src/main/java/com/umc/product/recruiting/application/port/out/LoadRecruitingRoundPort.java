package com.umc.product.recruiting.application.port.out;

import java.util.List;
import java.util.Optional;

import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

public interface LoadRecruitingRoundPort {

    Optional<RecruitingRound> findById(Long id);

    RecruitingRound getById(Long id);

    RecruitingRound getByIdForUpdate(Long id);

    List<RecruitingRound> listBySeasonId(Long seasonId);

    List<RecruitingRound> listBySeasonIds(List<Long> seasonIds);

    boolean existsBySeasonIdAndTypeAndRoundNo(Long seasonId, RecruitingRoundType type, Integer roundNo);

    boolean existsBySeasonIdAndTitleIgnoreCase(Long seasonId, String title);

    boolean existsBySeasonIdAndTitleIgnoreCaseAndIdNot(Long seasonId, String title, Long id);

    int getMaxAdditionalRoundNo(Long seasonId);
}
