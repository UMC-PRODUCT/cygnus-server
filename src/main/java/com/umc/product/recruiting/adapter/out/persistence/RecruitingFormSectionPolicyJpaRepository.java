package com.umc.product.recruiting.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.recruiting.domain.RecruitingFormSectionPolicy;

public interface RecruitingFormSectionPolicyJpaRepository extends JpaRepository<RecruitingFormSectionPolicy, Long> {

    Optional<RecruitingFormSectionPolicy> findByFormSectionId(Long formSectionId);

    List<RecruitingFormSectionPolicy> findAllByApplicationForm_IdOrderByIdAsc(Long applicationFormId);
}
