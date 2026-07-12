package com.umc.product.recruiting.application.port.out;

import com.umc.product.recruiting.domain.RecruitingFormSectionPolicy;

public interface SaveRecruitingFormSectionPolicyPort {

    RecruitingFormSectionPolicy save(RecruitingFormSectionPolicy policy);
}
