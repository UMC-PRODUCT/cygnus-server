package com.umc.product.recruiting.application.service.query;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQuestionScopeUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationQuestionScopeInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingFormSectionPolicyPort;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingApplicationQuestionScopeQueryService
    implements GetRecruitingApplicationQuestionScopeUseCase {

    private final LoadRecruitingApplicationFormPort loadApplicationFormPort;
    private final LoadRecruitingFormSectionPolicyPort loadPolicyPort;
    private final GetFormUseCase getFormUseCase;

    @Override
    public RecruitingApplicationQuestionScopeInfo getQuestionScope(
        Long applicationFormId,
        ChallengerTrack firstChoice,
        ChallengerTrack secondChoice
    ) {
        RecruitingApplicationForm applicationForm = loadApplicationFormPort.getById(applicationFormId);
        Set<Long> selectedSectionIds = loadPolicyPort.listByApplicationFormId(applicationFormId).stream()
            .filter(policy -> policy.appliesTo(firstChoice, secondChoice))
            .map(policy -> policy.getFormSectionId())
            .collect(Collectors.toSet());
        var selectedSections = getFormUseCase.getFormWithStructure(applicationForm.getFormId()).sections().stream()
            .filter(section -> selectedSectionIds.contains(section.sectionId()))
            .toList();
        Set<Long> allowedQuestionIds = selectedSections.stream()
            .flatMap(section -> section.questions().stream())
            .map(question -> question.questionId())
            .collect(Collectors.toSet());
        Set<Long> requiredQuestionIds = selectedSections.stream()
            .flatMap(section -> section.questions().stream())
            .filter(question -> question.isRequired())
            .map(question -> question.questionId())
            .collect(Collectors.toSet());
        return new RecruitingApplicationQuestionScopeInfo(allowedQuestionIds, requiredQuestionIds);
    }
}
