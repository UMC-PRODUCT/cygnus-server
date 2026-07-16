package com.umc.product.recruiting.application.service.command;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.recruiting.application.port.in.command.ValidateRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingFormSectionPolicyPort;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingFormSectionPolicy;
import com.umc.product.recruiting.domain.enums.RecruitingFormSectionType;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

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
        FormWithStructureInfo structure = getFormUseCase.getFormWithStructure(applicationForm.getFormId());
        Set<Long> formSectionIds = structure.sections().stream()
            .map(section -> section.sectionId())
            .collect(Collectors.toSet());
        Map<Long, RecruitingFormSectionPolicy> policyBySectionId = policies.stream()
            .collect(Collectors.toMap(RecruitingFormSectionPolicy::getFormSectionId, Function.identity()));
        if (!policyBySectionId.keySet().equals(formSectionIds)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_FORM_SECTION_POLICY_INVALID);
        }
        validateConditionalTransitions(structure, policyBySectionId);
        applicationForm.validatePoliciesForPublish(policies);
    }

    private static void validateConditionalTransitions(
        FormWithStructureInfo structure,
        Map<Long, RecruitingFormSectionPolicy> policyBySectionId
    ) {
        structure.sections().forEach(section -> section.questions().forEach(question -> question.options().forEach(option -> {
            if (option.nextSectionId() == null) {
                return;
            }
            RecruitingFormSectionPolicy source = policyBySectionId.get(section.sectionId());
            RecruitingFormSectionPolicy target = policyBySectionId.get(option.nextSectionId());
            if (!isAllowedTransition(source, target)) {
                throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_FORM_SECTION_POLICY_INVALID);
            }
        })));
    }

    private static boolean isAllowedTransition(
        RecruitingFormSectionPolicy source,
        RecruitingFormSectionPolicy target
    ) {
        if (source == null || target == null) {
            return false;
        }
        if (target.getType() == RecruitingFormSectionType.COMMON) {
            return true;
        }
        return source.getType() == RecruitingFormSectionType.TRACK && source.getTrack() == target.getTrack();
    }
}
