package com.umc.product.recruiting.application.service.query;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQuestionScopeUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.ValidateRecruitingApplicationScopeUseCase;
import com.umc.product.recruiting.application.port.in.query.ValidateRecruitingFormScopeUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundStatusSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicationSummaryRow;
import com.umc.product.recruiting.domain.RecruitingApplicantProfile;
import com.umc.product.recruiting.domain.RecruitingApplication;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitingQueryService implements
    GetRecruitingApplicationQueryUseCase,
    GetRecruitingFormQueryUseCase,
    ValidateRecruitingApplicationScopeUseCase,
    ValidateRecruitingFormScopeUseCase {

    private final LoadRecruitingApplicationPort loadApplicationPort;
    private final LoadRecruitingRoundPort loadRoundPort;
    private final LoadRecruitingApplicationFormPort loadApplicationFormPort;
    private final GetChallengerRoleUseCase getChallengerRoleUseCase;
    private final GetFormUseCase getFormUseCase;
    private final GetRecruitingApplicationQuestionScopeUseCase getQuestionScopeUseCase;

    @Override
    public RecruitingApplicationInfo getById(Long applicationId, Long requesterMemberId) {
        RecruitingApplication application = loadApplicationPort.getById(applicationId);
        application.validateApplicant(requesterMemberId);
        return RecruitingApplicationInfo.from(application);
    }

    @Override
    public FormWithStructureInfo getPublicFormStructure(
        Long applicationFormId,
        ChallengerTrack firstChoice,
        ChallengerTrack secondChoice
    ) {
        RecruitingApplicationForm applicationForm = loadApplicationFormPort.getById(applicationFormId);
        if (applicationForm.getStatus() != RecruitingApplicationFormStatus.PUBLISHED) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_NOT_PUBLISHED);
        }
        RecruitingApplicantProfile.validateChoices(applicationForm.getRound(), firstChoice, secondChoice);
        var scope = getQuestionScopeUseCase.getQuestionScope(applicationFormId, firstChoice, secondChoice);
        FormWithStructureInfo structure = getFormUseCase.getFormWithStructureByQuestionIds(
            applicationForm.getFormId(),
            scope.allowedQuestionIds()
        );
        List<FormWithStructureInfo.SectionWithQuestions> visibleSections = structure.sections().stream()
            .filter(section -> !section.questions().isEmpty())
            .toList();
        Set<Long> visibleSectionIds = visibleSections.stream()
            .map(FormWithStructureInfo.SectionWithQuestions::sectionId)
            .collect(java.util.stream.Collectors.toSet());
        return FormWithStructureInfo.builder()
            .formId(structure.formId())
            .createdMemberId(structure.createdMemberId())
            .title(structure.title())
            .description(structure.description())
            .status(structure.status())
            .isAnonymous(structure.isAnonymous())
            .allowDuplicateResponses(structure.allowDuplicateResponses())
            .createdAt(structure.createdAt())
            .updatedAt(structure.updatedAt())
            .sections(visibleSections.stream()
                .map(section -> filterConditionalDestinations(section, visibleSectionIds))
                .toList())
            .build();
    }

    private FormWithStructureInfo.SectionWithQuestions filterConditionalDestinations(
        FormWithStructureInfo.SectionWithQuestions section,
        Set<Long> visibleSectionIds
    ) {
        return FormWithStructureInfo.SectionWithQuestions.builder()
            .sectionId(section.sectionId())
            .title(section.title())
            .description(section.description())
            .orderNo(section.orderNo())
            .questions(section.questions().stream()
                .map(question -> FormWithStructureInfo.QuestionWithOptions.builder()
                    .questionId(question.questionId())
                    .title(question.title())
                    .description(question.description())
                    .type(question.type())
                    .isRequired(question.isRequired())
                    .orderNo(question.orderNo())
                    .options(question.options().stream()
                        .filter(option -> option.nextSectionId() == null
                            || visibleSectionIds.contains(option.nextSectionId()))
                        .toList())
                    .build())
                .toList())
            .build();
    }

    @Override
    public RecruitingStatusSummaryInfo getStatusSummary(Long gisuId, Long schoolId, Long requesterMemberId) {
        return getStatusSummary(gisuId, schoolId, null, requesterMemberId);
    }

    @Override
    public RecruitingStatusSummaryInfo getStatusSummary(
        Long gisuId,
        Long schoolId,
        Long roundId,
        Long requesterMemberId
    ) {
        validateCentralGisuAccess(requesterMemberId, gisuId);
        List<RecruitingApplicationSummaryRow> rows = loadApplicationPort.searchSummaryRows(
            gisuId,
            schoolId,
            roundId,
            null
        );
        Map<RecruitingApplicationStatus, Long> countByStatus = new EnumMap<>(RecruitingApplicationStatus.class);
        for (RecruitingApplicationSummaryRow row : rows) {
            countByStatus.merge(row.applicationStatus(), 1L, Long::sum);
        }
        Map<Long, List<RecruitingApplicationSummaryRow>> rowsByRound = rows.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                RecruitingApplicationSummaryRow::roundId,
                LinkedHashMap::new,
                java.util.stream.Collectors.toList()
            ));
        List<RecruitingRoundStatusSummaryInfo> rounds = rowsByRound.values().stream()
            .map(roundRows -> {
                RecruitingApplicationSummaryRow first = roundRows.getFirst();
                Map<RecruitingApplicationStatus, Long> roundCountByStatus = new EnumMap<>(
                    RecruitingApplicationStatus.class
                );
                roundRows.forEach(row -> roundCountByStatus.merge(row.applicationStatus(), 1L, Long::sum));
                return new RecruitingRoundStatusSummaryInfo(
                    first.roundId(),
                    first.roundType(),
                    first.roundNo(),
                    (long) roundRows.size(),
                    roundCountByStatus
                );
            })
            .toList();
        return new RecruitingStatusSummaryInfo((long) rows.size(), countByStatus, rounds);
    }

    private void validateCentralGisuAccess(Long requesterMemberId, Long gisuId) {
        if (getChallengerRoleUseCase.isCentralCoreInGisu(requesterMemberId, gisuId)
            || getChallengerRoleUseCase.isSuperAdmin(requesterMemberId)) {
            return;
        }
        throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_SUMMARY_ACCESS_DENIED);
    }

    @Override
    public boolean isRoundBelongsToSeason(Long roundId, Long seasonId) {
        if (roundId == null || seasonId == null) {
            return false;
        }
        return Objects.equals(loadRoundPort.getById(roundId).getSeason().getId(), seasonId);
    }

    @Override
    public boolean isApplicationBelongsToSeason(Long applicationId, Long seasonId) {
        if (applicationId == null || seasonId == null) {
            return false;
        }
        RecruitingApplication application = loadApplicationPort.getById(applicationId);
        return Objects.equals(application.getRound().getSeason().getId(), seasonId);
    }

    @Override
    public void validateRoundScope(Long applicationId, Long roundId) {
        RecruitingApplication application = loadApplicationPort.getById(applicationId);
        if (!Objects.equals(application.getRound().getId(), roundId)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_NOT_FOUND);
        }
    }

    @Override
    public boolean isApplicationFormBelongsToSeason(Long applicationFormId, Long seasonId) {
        if (applicationFormId == null || seasonId == null) {
            return false;
        }
        return Objects.equals(
            loadApplicationFormPort.getById(applicationFormId).getRound().getSeason().getId(),
            seasonId
        );
    }

    @Override
    public void validateSeasonScope(Long applicationFormId, Long seasonId) {
        if (!isApplicationFormBelongsToSeason(applicationFormId, seasonId)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_NOT_FOUND);
        }
    }

    @Override
    public boolean isFormBelongsToSeason(Long formId, Long seasonId) {
        if (formId == null || seasonId == null) {
            return false;
        }
        return loadApplicationFormPort.existsByFormIdAndSeasonId(formId, seasonId);
    }

}
