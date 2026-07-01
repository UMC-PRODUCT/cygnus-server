package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LoadRecruitingSeasonPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingSeasonPort;
import com.umc.product.recruiting.domain.RecruitingSeason;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingSeasonPersistenceAdapter implements LoadRecruitingSeasonPort, SaveRecruitingSeasonPort {

    private final RecruitingSeasonJpaRepository recruitingSeasonJpaRepository;

    @Override
    public Optional<RecruitingSeason> findById(Long id) {
        return recruitingSeasonJpaRepository.findById(id);
    }

    @Override
    public RecruitingSeason getById(Long id) {
        return findById(id)
            .orElseThrow(() -> new RecruitingDomainException(RecruitingErrorCode.RECRUITING_SEASON_NOT_FOUND));
    }

    @Override
    public Optional<RecruitingSeason> findByGisuIdAndSchoolId(Long gisuId, Long schoolId) {
        return recruitingSeasonJpaRepository.findByGisuIdAndSchoolId(gisuId, schoolId);
    }

    @Override
    public boolean existsByGisuIdAndSchoolId(Long gisuId, Long schoolId) {
        return recruitingSeasonJpaRepository.existsByGisuIdAndSchoolId(gisuId, schoolId);
    }

    @Override
    public List<RecruitingSeason> listByGisuId(Long gisuId) {
        return recruitingSeasonJpaRepository.findAllByGisuIdOrderBySchoolIdAscIdAsc(gisuId);
    }

    @Override
    public RecruitingSeason save(RecruitingSeason season) {
        return recruitingSeasonJpaRepository.save(season);
    }
}
