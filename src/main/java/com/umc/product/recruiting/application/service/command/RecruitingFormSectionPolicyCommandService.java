package com.umc.product.recruiting.application.service.command;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingFormSectionPolicyUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.AddRecruitingFormSectionPolicyCommand;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingFormSectionPolicyPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingFormSectionPolicyPort;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingFormSectionPolicy;
import com.umc.product.recruiting.domain.enums.RecruitingFormSectionType;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingFormSectionPolicyCommandService implements ManageRecruitingFormSectionPolicyUseCase {

    private final LoadRecruitingApplicationFormPort loadApplicationFormPort;
    private final LoadRecruitingFormSectionPolicyPort loadPolicyPort;
    private final SaveRecruitingFormSectionPolicyPort savePolicyPort;
    private final GetFormUseCase getFormUseCase;

    @Override
    public Long addPolicy(AddRecruitingFormSectionPolicyCommand command) {
        RecruitingApplicationForm applicationForm = loadApplicationFormPort.getById(command.applicationFormId());
        applicationForm.validateStructureMutable();
        if (loadPolicyPort.findByFormSectionId(command.formSectionId()).isPresent()) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_FORM_SECTION_POLICY_INVALID);
        }
        validateSectionBelongsToForm(applicationForm, command.formSectionId());
        RecruitingFormSectionPolicy policy = createPolicy(applicationForm, command);
        return savePolicyPort.save(policy).getId();
    }

    private RecruitingFormSectionPolicy createPolicy(
        RecruitingApplicationForm applicationForm,
        AddRecruitingFormSectionPolicyCommand command
    ) {
        if (command.type() == RecruitingFormSectionType.COMMON) {
            return RecruitingFormSectionPolicy.createCommon(applicationForm, command.formSectionId());
        }
        if (command.type() == RecruitingFormSectionType.TRACK) {
            return RecruitingFormSectionPolicy.createTrack(applicationForm, command.formSectionId(), command.track());
        }
        throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_FORM_SECTION_POLICY_INVALID);
    }

    private void validateSectionBelongsToForm(RecruitingApplicationForm applicationForm, Long formSectionId) {
        Set<Long> sectionIds = getFormUseCase.getFormWithStructure(applicationForm.getFormId()).sections().stream()
            .map(section -> section.sectionId())
            .collect(Collectors.toSet());
        if (!sectionIds.contains(formSectionId)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_FORM_SECTION_POLICY_INVALID);
        }
    }
}
