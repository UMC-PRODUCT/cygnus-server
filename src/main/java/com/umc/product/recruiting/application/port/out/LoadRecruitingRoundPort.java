package com.umc.product.recruiting.application.port.out;

import java.util.List;
import java.util.Optional;

import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

/**
 * 삭제된(soft delete) 차수는 모든 조회에서 제외한다.
 * 삭제된 차수에 접근해야 하는 복구 경로만 {@link #getByIdForUpdateIncludingDeleted}를 사용하며,
 * 그 외 경로는 삭제된 차수를 조회할 수 없어 자연히 {@code RECRUITING_ROUND_NOT_FOUND}로 처리된다.
 */
public interface LoadRecruitingRoundPort {

    Optional<RecruitingRound> findById(Long id);

    RecruitingRound getById(Long id);

    RecruitingRound getByIdForUpdate(Long id);

    /** 복구 전용. 삭제된 차수까지 포함해 조회한다. */
    RecruitingRound getByIdForUpdateIncludingDeleted(Long id);

    List<RecruitingRound> listBySeasonId(Long seasonId);

    List<RecruitingRound> listBySeasonIds(List<Long> seasonIds);

    boolean existsBySeasonIdAndTypeAndRoundNo(Long seasonId, RecruitingRoundType type, Integer roundNo);

    boolean existsBySeasonIdAndTitleIgnoreCase(Long seasonId, String title);

    boolean existsBySeasonIdAndTitleIgnoreCaseAndIdNot(Long seasonId, String title, Long id);

    int getMaxAdditionalRoundNo(Long seasonId);
}
