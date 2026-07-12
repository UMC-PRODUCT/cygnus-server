package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationFormPort;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingApplicationFormPersistenceAdapter
    implements LoadRecruitingApplicationFormPort, SaveRecruitingApplicationFormPort {

    private final RecruitingApplicationFormJpaRepository recruitingApplicationFormJpaRepository;

    @Override
    public Optional<RecruitingApplicationForm> findById(Long id) {
        return recruitingApplicationFormJpaRepository.findById(id);
    }

    @Override
    public RecruitingApplicationForm getById(Long id) {
        return findById(id)
            .orElseThrow(() -> new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_NOT_FOUND));
    }

    @Override
    public RecruitingApplicationForm getByIdForUpdate(Long id) {
        return recruitingApplicationFormJpaRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_NOT_FOUND));
    }

    @Override
    public Optional<RecruitingApplicationForm> findByRoundId(Long roundId) {
        return recruitingApplicationFormJpaRepository.findByRound_Id(roundId);
    }

    @Override
    public Optional<RecruitingApplicationForm> findByFormId(Long formId) {
        return recruitingApplicationFormJpaRepository.findFirstByFormIdOrderByIdAsc(formId);
    }

    @Override
    public boolean existsByFormIdAndSeasonId(Long formId, Long seasonId) {
        return recruitingApplicationFormJpaRepository.existsByFormIdAndRound_Season_Id(formId, seasonId);
    }

    @Override
    public List<RecruitingApplicationForm> listByRoundIdsAndStatus(
        List<Long> roundIds,
        RecruitingApplicationFormStatus status
    ) {
        if (roundIds.isEmpty()) {
            return List.of();
        }
        return recruitingApplicationFormJpaRepository.findAllByRoundIdsAndStatus(roundIds, status);
    }

    @Override
    public RecruitingApplicationForm save(RecruitingApplicationForm applicationForm) {
        return recruitingApplicationFormJpaRepository.save(applicationForm);
    }
}
