package com.umc.product.recruiting.application.port.out;

import java.util.List;
import java.util.Optional;

import com.umc.product.recruiting.domain.RecruitingSeason;

public interface LoadRecruitingSeasonPort {

    Optional<RecruitingSeason> findById(Long id);

    RecruitingSeason getById(Long id);

    Optional<RecruitingSeason> findByGisuIdAndSchoolId(Long gisuId, Long schoolId);

    boolean existsByGisuIdAndSchoolId(Long gisuId, Long schoolId);

    List<RecruitingSeason> listByGisuId(Long gisuId);
}
