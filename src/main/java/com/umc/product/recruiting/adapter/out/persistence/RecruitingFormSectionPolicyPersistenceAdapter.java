package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.recruiting.application.port.out.LoadRecruitingFormSectionPolicyPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingFormSectionPolicyPort;
import com.umc.product.recruiting.domain.RecruitingFormSectionPolicy;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecruitingFormSectionPolicyPersistenceAdapter
    implements LoadRecruitingFormSectionPolicyPort, SaveRecruitingFormSectionPolicyPort {

    private final RecruitingFormSectionPolicyJpaRepository repository;

    @Override
    public Optional<RecruitingFormSectionPolicy> findByFormSectionId(Long formSectionId) {
        return repository.findByFormSectionId(formSectionId);
    }

    @Override
    public List<RecruitingFormSectionPolicy> listByApplicationFormId(Long applicationFormId) {
        return repository.findAllByApplicationForm_IdOrderByIdAsc(applicationFormId);
    }

    @Override
    public RecruitingFormSectionPolicy save(RecruitingFormSectionPolicy policy) {
        return repository.save(policy);
    }

    @Override
    public void deleteByFormSectionId(Long formSectionId) {
        repository.findByFormSectionId(formSectionId).ifPresent(repository::delete);
    }

    @Override
    public void deleteByApplicationFormId(Long applicationFormId) {
        repository.deleteAll(repository.findAllByApplicationForm_IdOrderByIdAsc(applicationFormId));
    }
}
