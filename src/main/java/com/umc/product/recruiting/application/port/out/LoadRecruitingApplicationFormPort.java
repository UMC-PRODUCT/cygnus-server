package com.umc.product.recruiting.application.port.out;

import java.util.List;
import java.util.Optional;

import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;

public interface LoadRecruitingApplicationFormPort {

    Optional<RecruitingApplicationForm> findById(Long id);

    RecruitingApplicationForm getById(Long id);

    Optional<RecruitingApplicationForm> findByRoundId(Long roundId);

    Optional<RecruitingApplicationForm> findByFormId(Long formId);

    boolean existsByFormIdAndSeasonId(Long formId, Long seasonId);

    List<RecruitingApplicationForm> listByRoundIdsAndStatus(
        List<Long> roundIds,
        RecruitingApplicationFormStatus status
    );
}
