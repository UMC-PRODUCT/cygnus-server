package com.umc.product.recruiting.application.service.command;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.recruiting.application.port.in.command.ValidateRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingFormSectionPolicyPort;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingFormSectionPolicy;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;
import com.umc.product.survey.application.port.in.query.GetFormUseCase;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingApplicationFormValidationService implements ValidateRecruitingApplicationFormUseCase {

    private final LoadRecruitingApplicationFormPort loadApplicationFormPort;
    private final LoadRecruitingFormSectionPolicyPort loadPolicyPort;
    private final GetFormUseCase getFormUseCase;

    @Override
    public void validateForPublish(Long applicationFormId) {
        RecruitingApplicationForm applicationForm = loadApplicationFormPort.getById(applicationFormId);
        List<RecruitingFormSectionPolicy> policies = loadPolicyPort.listByApplicationFormId(applicationFormId);
        Set<Long> formSectionIds = getFormUseCase.getFormWithStructure(applicationForm.getFormId()).sections().stream()
            .map(section -> section.sectionId())
            .collect(Collectors.toSet());
        if (policies.stream().map(RecruitingFormSectionPolicy::getFormSectionId).anyMatch(id -> !formSectionIds.contains(id))) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_FORM_SECTION_POLICY_INVALID);
        }
        applicationForm.validatePoliciesForPublish(policies);
    }
}
