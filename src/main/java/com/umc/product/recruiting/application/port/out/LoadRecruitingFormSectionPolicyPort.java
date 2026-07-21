package com.umc.product.recruiting.application.port.out;

import java.util.List;
import java.util.Optional;

import com.umc.product.recruiting.domain.RecruitingFormSectionPolicy;

public interface LoadRecruitingFormSectionPolicyPort {

    Optional<RecruitingFormSectionPolicy> findByFormSectionId(Long formSectionId);

    List<RecruitingFormSectionPolicy> listByApplicationFormId(Long applicationFormId);
}
