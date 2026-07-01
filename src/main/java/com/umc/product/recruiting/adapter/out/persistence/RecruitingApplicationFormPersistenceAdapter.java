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
    public Optional<RecruitingApplicationForm> findByRoundIdAndFormId(Long roundId, Long formId) {
        return recruitingApplicationFormJpaRepository.findByRound_IdAndFormId(roundId, formId);
    }

    @Override
    public Optional<RecruitingApplicationForm> findByFormId(Long formId) {
        return recruitingApplicationFormJpaRepository.findFirstByFormIdOrderByIdAsc(formId);
    }

    @Override
    public List<RecruitingApplicationForm> listByRoundId(Long roundId) {
        return recruitingApplicationFormJpaRepository.findAllByRound_IdOrderByIdAsc(roundId);
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
