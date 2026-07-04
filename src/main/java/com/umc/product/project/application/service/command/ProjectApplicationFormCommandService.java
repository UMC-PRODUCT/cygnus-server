package com.umc.product.project.application.service.command;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.project.application.port.in.command.UpsertProjectApplicationFormUseCase;
import com.umc.product.project.application.port.in.command.dto.UpsertApplicationFormCommand;
import com.umc.product.project.application.port.in.command.dto.UpsertApplicationFormCommand.ApplicationFormSectionEntry;
import com.umc.product.project.application.port.in.command.dto.UpsertApplicationFormCommand.ApplicationQuestionEntry;
import com.umc.product.project.application.port.in.command.dto.UpsertApplicationFormCommand.ApplicationQuestionOptionEntry;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPolicyPort;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPort;
import com.umc.product.project.application.port.out.LoadProjectMatchingRoundPort;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.application.port.out.SaveProjectApplicationFormPolicyPort;
import com.umc.product.project.application.port.out.SaveProjectApplicationFormPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectApplicationFormPolicy;
import com.umc.product.project.domain.enums.FormSectionType;
import com.umc.product.project.domain.enums.ProjectStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;
import com.umc.product.form.application.port.in.command.ManageFormSectionUseCase;
import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.form.application.port.in.command.ManageQuestionOptionUseCase;
import com.umc.product.form.application.port.in.command.ManageQuestionUseCase;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormCommand;
import com.umc.product.form.application.port.in.command.dto.CreateFormSectionCommand;
import com.umc.product.form.application.port.in.command.dto.CreateQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.CreateQuestionOptionCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteFormSectionCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteQuestionOptionCommand;
import com.umc.product.form.application.port.in.command.dto.ForkQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.ReorderFormSectionsCommand;
import com.umc.product.form.application.port.in.command.dto.ReorderQuestionOptionsCommand;
import com.umc.product.form.application.port.in.command.dto.ReorderQuestionsCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateFormCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateFormSectionCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateQuestionOptionCommand;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo.Option;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo.QuestionWithOptions;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo.SectionWithQuestions;
import com.umc.product.form.domain.enums.QuestionType;

import lombok.RequiredArgsConstructor;

/**
 * 지원 폼 upsert 서비스 (PROJECT-106).
 * <p>
 * 본문이 곧 폼의 새 상태가 되도록 (PUT 시멘틱) 섹션/질문/옵션을 3계층 diff 로 동기화한다.
 * Form 도메인의 5종 UseCase 와 Project 도메인의 정책({@link ProjectApplicationFormPolicy})
 * 을 합쳐 단일 트랜잭션으로 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectApplicationFormCommandService implements UpsertProjectApplicationFormUseCase {

    private static final String DEFAULT_FORM_TITLE = "프로젝트 지원서";
    private static final Set<QuestionType> CHOICE_QUESTION_TYPES =
        Set.of(QuestionType.RADIO, QuestionType.CHECKBOX, QuestionType.DROPDOWN);

    private final LoadProjectPort loadProjectPort;
    private final LoadProjectMatchingRoundPort loadMatchingRoundPort;
    private final LoadProjectApplicationFormPort loadApplicationFormPort;
    private final SaveProjectApplicationFormPort saveApplicationFormPort;
    private final LoadProjectApplicationFormPolicyPort loadPolicyPort;
    private final SaveProjectApplicationFormPolicyPort savePolicyPort;

    // Cross-domain
    private final ManageFormUseCase manageFormUseCase;
    private final ManageFormSectionUseCase manageFormSectionUseCase;
    private final ManageQuestionUseCase manageQuestionUseCase;
    private final ManageQuestionOptionUseCase manageQuestionOptionUseCase;
    private final GetFormUseCase getFormUseCase;

    @Override
    public ApplicationFormInfo upsert(UpsertApplicationFormCommand command) {
        validateOptionTypeRules(command.sections());

        Project project = loadProjectPort.getById(command.projectId());
        boolean hasActiveRound = !loadMatchingRoundPort.listOpenAt(project.getChapterId(), Instant.now()).isEmpty();
        project.validateApplicationFormEditable(hasActiveRound);

        boolean shouldFork = project.getStatus() == ProjectStatus.IN_PROGRESS;

        ProjectApplicationForm applicationForm;
        FormWithStructureInfo existingStructure;

        var maybeExisting = loadApplicationFormPort.findByProjectId(command.projectId());
        if (maybeExisting.isPresent()) {
            applicationForm = maybeExisting.get();
            // 메타 + 구조를 한 번에 가져와 syncFormMetaIfChanged / applyDiff 둘 다 재사용 (cross-domain 호출 1회 절감)
            existingStructure = getFormUseCase.getFormWithStructure(applicationForm.getFormId());
            syncFormMetaIfChanged(applicationForm, existingStructure, project, command);
        } else {
            applicationForm = createApplicationForm(project, command);
            // 신규 폼은 비어있는 구조 — 호출 없이 직접 생성
            existingStructure = emptyStructureFor(applicationForm.getFormId());
        }

        applyDiff(applicationForm, existingStructure, command, shouldFork);

        return assembleResponse(applicationForm);
    }

    private FormWithStructureInfo emptyStructureFor(Long formId) {
        return FormWithStructureInfo.builder()
            .formId(formId)
            .sections(List.of())
            .build();
    }

    /* =====================================================
     * 1) 폼 라이프사이클 (생성 / 메타 sync)
     * ===================================================== */

    private ProjectApplicationForm createApplicationForm(Project project, UpsertApplicationFormCommand command) {
        Long formId = manageFormUseCase.createDraft(
            CreateDraftFormCommand.builder()
                .createdMemberId(command.requesterMemberId())
                .title(resolveTitle(project, command))
                .description(command.description())
                .isAnonymous(false)
                .allowDuplicateResponses(true)
                .build()
        );
        return saveApplicationFormPort.save(ProjectApplicationForm.create(project, formId));
    }

    private void syncFormMetaIfChanged(
        ProjectApplicationForm applicationForm,
        FormWithStructureInfo existing,
        Project project,
        UpsertApplicationFormCommand command
    ) {
        String resolvedTitle = resolveTitle(project, command);

        if (Objects.equals(existing.title(), resolvedTitle)
            && Objects.equals(existing.description(), command.description())) {
            return;
        }

        manageFormUseCase.updateForm(
            UpdateFormCommand.builder()
                .formId(applicationForm.getFormId())
                .requesterMemberId(command.requesterMemberId())
                .title(resolvedTitle)
                .description(command.description())
                .clearDescription(command.description() == null)
                .isAnonymous(existing.isAnonymous())
                .allowDuplicateResponses(existing.allowDuplicateResponses())
                .build()
        );
    }

    private String resolveTitle(Project project, UpsertApplicationFormCommand command) {
        if (command.title() != null) {
            return command.title();
        }
        if (project.getName() != null) {
            return project.getName();
        }
        return DEFAULT_FORM_TITLE;
    }

    /* =====================================================
     * 2) 3계층 diff (section -> question -> option)
     * ===================================================== */

    private void applyDiff(
        ProjectApplicationForm applicationForm,
        FormWithStructureInfo existing,
        UpsertApplicationFormCommand command,
        boolean shouldFork
    ) {
        Map<Long, SectionWithQuestions> existingSectionById = existing.sections().stream()
            .collect(Collectors.toMap(SectionWithQuestions::sectionId, Function.identity()));
        Map<Long, ProjectApplicationFormPolicy> policyByFormSectionId =
            loadPolicyPort.listByApplicationFormId(applicationForm.getId()).stream()
                .collect(Collectors.toMap(ProjectApplicationFormPolicy::getFormSectionId, Function.identity()));

        validateSectionIds(command.sections(), existingSectionById.keySet());
        validateQuestionAndOptionIds(command.sections(), existingSectionById);

        List<Long> orderedSectionIds = applySectionDiff(
            applicationForm, command, existingSectionById, policyByFormSectionId, shouldFork
        );

        deleteRemovedSections(command.sections(), existing.sections(), command.requesterMemberId());

        if (!orderedSectionIds.isEmpty()) {
            manageFormSectionUseCase.reorderSections(
                ReorderFormSectionsCommand.builder()
                    .formId(applicationForm.getFormId())
                    .requesterMemberId(command.requesterMemberId())
                    .orderedSectionIds(orderedSectionIds)
                    .build()
            );
        }
    }

    private List<Long> applySectionDiff(
        ProjectApplicationForm applicationForm,
        UpsertApplicationFormCommand command,
        Map<Long, SectionWithQuestions> existingSectionById,
        Map<Long, ProjectApplicationFormPolicy> policyByFormSectionId,
        boolean shouldFork
    ) {
        List<Long> orderedSectionIds = new ArrayList<>();
        for (ApplicationFormSectionEntry entry : command.sections()) {
            Long sectionId = (entry.sectionId() == null)
                ? createNewSection(applicationForm, entry, command.requesterMemberId())
                : updateExistingSection(
                    entry,
                    existingSectionById.get(entry.sectionId()),
                    policyByFormSectionId.get(entry.sectionId()),
                    command.requesterMemberId(),
                    shouldFork
                );
            orderedSectionIds.add(sectionId);
        }
        return orderedSectionIds;
    }

    private Long createNewSection(
        ProjectApplicationForm applicationForm,
        ApplicationFormSectionEntry entry,
        Long requesterMemberId
    ) {
        Long sectionId = manageFormSectionUseCase.createSection(
            CreateFormSectionCommand.builder()
                .formId(applicationForm.getFormId())
                .requesterMemberId(requesterMemberId)
                .title(entry.title())
                .description(entry.description())
                .build()
        );

        savePolicyPort.save(buildPolicy(applicationForm, sectionId, entry));

        // 본문 순서대로 reorder 명시 호출 — Form 단의 자동 orderNo 부여 정책에 의존하지 않음
        List<Long> newQuestionIds = new ArrayList<>();
        for (ApplicationQuestionEntry questionEntry : entry.questions()) {
            newQuestionIds.add(createNewQuestion(sectionId, questionEntry, requesterMemberId));
        }
        if (!newQuestionIds.isEmpty()) {
            manageQuestionUseCase.reorderQuestions(
                ReorderQuestionsCommand.builder()
                    .sectionId(sectionId)
                    .requesterMemberId(requesterMemberId)
                    .orderedQuestionIds(newQuestionIds)
                    .build()
            );
        }

        return sectionId;
    }

    private Long updateExistingSection(
        ApplicationFormSectionEntry entry,
        SectionWithQuestions existingSection,
        ProjectApplicationFormPolicy existingPolicy,
        Long requesterMemberId,
        boolean shouldFork
    ) {
        if (sectionMetaChanged(existingSection, entry)) {
            manageFormSectionUseCase.updateSection(
                UpdateFormSectionCommand.builder()
                    .sectionId(entry.sectionId())
                    .requesterMemberId(requesterMemberId)
                    .title(entry.title())
                    .description(entry.description())
                    .clearDescription(entry.description() == null)
                    .build()
            );
        }

        if (policyChanged(existingPolicy, entry)) {
            existingPolicy.updatePolicy(entry.type(), nullSafeAllowedParts(entry));
            savePolicyPort.save(existingPolicy);
        }

        applyQuestionDiff(entry, existingSection, requesterMemberId, shouldFork);
        return entry.sectionId();
    }

    private void deleteRemovedSections(
        List<ApplicationFormSectionEntry> requestSections,
        List<SectionWithQuestions> existingSections,
        Long requesterMemberId
    ) {
        Set<Long> requestedExistingIds = requestSections.stream()
            .map(ApplicationFormSectionEntry::sectionId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        for (SectionWithQuestions existing : existingSections) {
            if (requestedExistingIds.contains(existing.sectionId())) {
                continue;
            }
            manageFormSectionUseCase.deleteSection(
                DeleteFormSectionCommand.builder()
                    .sectionId(existing.sectionId())
                    .requesterMemberId(requesterMemberId)
                    .build()
            );
            savePolicyPort.deleteByFormSectionId(existing.sectionId());
        }
    }

    private void applyQuestionDiff(
        ApplicationFormSectionEntry sectionEntry,
        SectionWithQuestions existingSection,
        Long requesterMemberId,
        boolean shouldFork
    ) {
        Map<Long, QuestionWithOptions> existingQuestionById = existingSection.questions().stream()
            .collect(Collectors.toMap(QuestionWithOptions::questionId, Function.identity()));

        List<Long> orderedQuestionIds = new ArrayList<>();
        for (ApplicationQuestionEntry questionEntry : sectionEntry.questions()) {
            Long questionId = (questionEntry.questionId() == null)
                ? createNewQuestion(sectionEntry.sectionId(), questionEntry, requesterMemberId)
                : updateExistingQuestion(
                    questionEntry,
                    existingQuestionById.get(questionEntry.questionId()),
                    requesterMemberId,
                    shouldFork
                );
            orderedQuestionIds.add(questionId);
        }

        deleteRemovedQuestions(sectionEntry.questions(), existingSection.questions(), requesterMemberId, shouldFork);

        if (!orderedQuestionIds.isEmpty()) {
            manageQuestionUseCase.reorderQuestions(
                ReorderQuestionsCommand.builder()
                    .sectionId(sectionEntry.sectionId())
                    .requesterMemberId(requesterMemberId)
                    .orderedQuestionIds(orderedQuestionIds)
                    .build()
            );
        }
    }

    private Long createNewQuestion(Long sectionId, ApplicationQuestionEntry entry, Long requesterMemberId) {
        Long questionId = manageQuestionUseCase.createQuestion(
            CreateQuestionCommand.builder()
                .sectionId(sectionId)
                .requesterMemberId(requesterMemberId)
                .type(entry.type())
                .title(entry.title())
                .description(entry.description())
                .isRequired(entry.isRequired())
                .build()
        );

        // 본문 순서대로 reorder 명시 호출 — Form 단의 자동 orderNo 부여 정책에 의존하지 않음
        List<Long> newOptionIds = new ArrayList<>();
        for (ApplicationQuestionOptionEntry optionEntry : entry.options()) {
            newOptionIds.add(createNewOption(questionId, optionEntry, requesterMemberId));
        }
        if (!newOptionIds.isEmpty()) {
            manageQuestionOptionUseCase.reorderOptions(
                ReorderQuestionOptionsCommand.builder()
                    .questionId(questionId)
                    .requesterMemberId(requesterMemberId)
                    .orderedOptionIds(newOptionIds)
                    .build()
            );
        }

        return questionId;
    }

    private Long updateExistingQuestion(
        ApplicationQuestionEntry entry,
        QuestionWithOptions existingQuestion,
        Long requesterMemberId,
        boolean shouldFork
    ) {
        if (questionMetaChanged(existingQuestion, entry)) {
            if (shouldFork) {
                Long newQuestionId = manageQuestionUseCase.forkQuestion(
                    ForkQuestionCommand.builder()
                        .originQuestionId(entry.questionId())
                        .requesterMemberId(requesterMemberId)
                        .build()
                );
                manageQuestionUseCase.updateQuestion(
                    UpdateQuestionCommand.builder()
                        .questionId(newQuestionId)
                        .requesterMemberId(requesterMemberId)
                        .type(entry.type())
                        .title(entry.title())
                        .description(entry.description())
                        .clearDescription(entry.description() == null)
                        .isRequired(entry.isRequired())
                        .build()
                );
                List<Long> newOptionIds = new ArrayList<>();
                for (ApplicationQuestionOptionEntry optionEntry : entry.options()) {
                    newOptionIds.add(createNewOption(newQuestionId, optionEntry, requesterMemberId));
                }
                if (!newOptionIds.isEmpty()) {
                    manageQuestionOptionUseCase.reorderOptions(
                        ReorderQuestionOptionsCommand.builder()
                            .questionId(newQuestionId)
                            .requesterMemberId(requesterMemberId)
                            .orderedOptionIds(newOptionIds)
                            .build()
                    );
                }
                return newQuestionId;
            }
            manageQuestionUseCase.updateQuestion(
                UpdateQuestionCommand.builder()
                    .questionId(entry.questionId())
                    .requesterMemberId(requesterMemberId)
                    .type(entry.type())
                    .title(entry.title())
                    .description(entry.description())
                    .clearDescription(entry.description() == null)
                    .isRequired(entry.isRequired())
                    .build()
            );
        }

        applyOptionDiff(entry, existingQuestion, requesterMemberId);
        return entry.questionId();
    }

    private void deleteRemovedQuestions(
        List<ApplicationQuestionEntry> requestQuestions,
        List<QuestionWithOptions> existingQuestions,
        Long requesterMemberId,
        boolean shouldFork
    ) {
        Set<Long> requestedExistingIds = requestQuestions.stream()
            .map(ApplicationQuestionEntry::questionId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        for (QuestionWithOptions existing : existingQuestions) {
            if (requestedExistingIds.contains(existing.questionId())) {
                continue;
            }
            if (shouldFork) {
                // 차수 사이 수정: 기존 응답자의 Answer 보존을 위해 물리 삭제 대신 비활성화
                manageQuestionUseCase.deactivateQuestion(existing.questionId());
            } else {
                manageQuestionUseCase.deleteQuestion(
                    DeleteQuestionCommand.builder()
                        .questionId(existing.questionId())
                        .requesterMemberId(requesterMemberId)
                        .build()
                );
            }
        }
    }

    private void applyOptionDiff(
        ApplicationQuestionEntry questionEntry,
        QuestionWithOptions existingQuestion,
        Long requesterMemberId
    ) {
        Map<Long, Option> existingOptionById = existingQuestion.options().stream()
            .collect(Collectors.toMap(Option::optionId, Function.identity()));

        List<Long> orderedOptionIds = new ArrayList<>();
        for (ApplicationQuestionOptionEntry optionEntry : questionEntry.options()) {
            Long optionId = (optionEntry.optionId() == null)
                ? createNewOption(questionEntry.questionId(), optionEntry, requesterMemberId)
                : updateExistingOption(
                    optionEntry,
                    existingOptionById.get(optionEntry.optionId()),
                    requesterMemberId
                );
            orderedOptionIds.add(optionId);
        }

        deleteRemovedOptions(questionEntry.options(), existingQuestion.options(), requesterMemberId);

        if (!orderedOptionIds.isEmpty()) {
            manageQuestionOptionUseCase.reorderOptions(
                ReorderQuestionOptionsCommand.builder()
                    .questionId(questionEntry.questionId())
                    .requesterMemberId(requesterMemberId)
                    .orderedOptionIds(orderedOptionIds)
                    .build()
            );
        }
    }

    private Long createNewOption(Long questionId, ApplicationQuestionOptionEntry entry, Long requesterMemberId) {
        return manageQuestionOptionUseCase.createOption(
            CreateQuestionOptionCommand.builder()
                .questionId(questionId)
                .requesterMemberId(requesterMemberId)
                .content(entry.content())
                .isOther(entry.isOther())
                .build()
        );
    }

    private Long updateExistingOption(
        ApplicationQuestionOptionEntry entry,
        Option existingOption,
        Long requesterMemberId
    ) {
        if (optionMetaChanged(existingOption, entry)) {
            manageQuestionOptionUseCase.updateOption(
                UpdateQuestionOptionCommand.builder()
                    .optionId(entry.optionId())
                    .requesterMemberId(requesterMemberId)
                    .content(entry.content())
                    .isOther(entry.isOther())
                    .build()
            );
        }
        return entry.optionId();
    }

    private void deleteRemovedOptions(
        List<ApplicationQuestionOptionEntry> requestOptions,
        List<Option> existingOptions,
        Long requesterMemberId
    ) {
        Set<Long> requestedExistingIds = requestOptions.stream()
            .map(ApplicationQuestionOptionEntry::optionId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        for (Option existing : existingOptions) {
            if (requestedExistingIds.contains(existing.optionId())) {
                continue;
            }
            manageQuestionOptionUseCase.deleteOption(
                DeleteQuestionOptionCommand.builder()
                    .optionId(existing.optionId())
                    .requesterMemberId(requesterMemberId)
                    .build()
            );
        }
    }

    /* =====================================================
     * 3) 검증
     * ===================================================== */

    private void validateOptionTypeRules(List<ApplicationFormSectionEntry> sections) {
        for (ApplicationFormSectionEntry section : sections) {
            for (ApplicationQuestionEntry question : section.questions()) {
                boolean isChoice = CHOICE_QUESTION_TYPES.contains(question.type());
                boolean hasOptions = !question.options().isEmpty();

                if (!isChoice && hasOptions) {
                    throw new ProjectDomainException(ProjectErrorCode.APPLICATION_FORM_OPTIONS_NOT_ALLOWED);
                }
                if (isChoice && !hasOptions) {
                    throw new ProjectDomainException(ProjectErrorCode.APPLICATION_FORM_OPTIONS_REQUIRED);
                }
            }
        }
    }

    private void validateSectionIds(List<ApplicationFormSectionEntry> sections, Set<Long> existingSectionIds) {
        for (ApplicationFormSectionEntry section : sections) {
            if (section.sectionId() != null && !existingSectionIds.contains(section.sectionId())) {
                throw new ProjectDomainException(ProjectErrorCode.APPLICATION_FORM_INVALID_SECTION_ID);
            }
        }
    }

    private void validateQuestionAndOptionIds(
        List<ApplicationFormSectionEntry> sections,
        Map<Long, SectionWithQuestions> existingSectionById
    ) {
        for (ApplicationFormSectionEntry sectionEntry : sections) {
            if (sectionEntry.sectionId() == null) {
                // 신규 섹션은 비교 대상이 없음 — 모든 questionId/optionId 가 null 이어야 함
                ensureAllQuestionIdsNull(sectionEntry.questions());
                continue;
            }
            SectionWithQuestions existingSection = existingSectionById.get(sectionEntry.sectionId());
            Map<Long, QuestionWithOptions> existingQuestionById = existingSection.questions().stream()
                .collect(Collectors.toMap(QuestionWithOptions::questionId, Function.identity()));

            for (ApplicationQuestionEntry questionEntry : sectionEntry.questions()) {
                if (questionEntry.questionId() != null
                    && !existingQuestionById.containsKey(questionEntry.questionId())) {
                    throw new ProjectDomainException(ProjectErrorCode.APPLICATION_FORM_INVALID_QUESTION_ID);
                }

                if (questionEntry.questionId() == null) {
                    ensureAllOptionIdsNull(questionEntry.options());
                    continue;
                }
                Set<Long> existingOptionIds = existingQuestionById.get(questionEntry.questionId()).options()
                    .stream().map(Option::optionId).collect(Collectors.toSet());
                for (ApplicationQuestionOptionEntry optionEntry : questionEntry.options()) {
                    if (optionEntry.optionId() != null
                        && !existingOptionIds.contains(optionEntry.optionId())) {
                        throw new ProjectDomainException(ProjectErrorCode.APPLICATION_FORM_INVALID_OPTION_ID);
                    }
                }
            }
        }
    }

    private void ensureAllQuestionIdsNull(List<ApplicationQuestionEntry> questions) {
        for (ApplicationQuestionEntry question : questions) {
            if (question.questionId() != null) {
                throw new ProjectDomainException(ProjectErrorCode.APPLICATION_FORM_INVALID_QUESTION_ID);
            }
            ensureAllOptionIdsNull(question.options());
        }
    }

    private void ensureAllOptionIdsNull(List<ApplicationQuestionOptionEntry> options) {
        for (ApplicationQuestionOptionEntry option : options) {
            if (option.optionId() != null) {
                throw new ProjectDomainException(ProjectErrorCode.APPLICATION_FORM_INVALID_OPTION_ID);
            }
        }
    }

    /* =====================================================
     * 4) Diff 비교 / 정책 빌드 헬퍼
     * ===================================================== */

    private boolean sectionMetaChanged(SectionWithQuestions existing, ApplicationFormSectionEntry entry) {
        return !Objects.equals(existing.title(), entry.title())
            || !Objects.equals(existing.description(), entry.description());
    }

    private boolean policyChanged(ProjectApplicationFormPolicy existing, ApplicationFormSectionEntry entry) {
        if (existing.getType() != entry.type()) {
            return true;
        }
        if (entry.type() == FormSectionType.COMMON) {
            return !existing.getAllowedParts().isEmpty();
        }
        return !new HashSet<>(existing.getAllowedParts()).equals(nullSafeAllowedParts(entry));
    }

    private boolean questionMetaChanged(QuestionWithOptions existing, ApplicationQuestionEntry entry) {
        return existing.type() != entry.type()
            || !Objects.equals(existing.title(), entry.title())
            || !Objects.equals(existing.description(), entry.description())
            || existing.isRequired() != entry.isRequired();
    }

    private boolean optionMetaChanged(Option existing, ApplicationQuestionOptionEntry entry) {
        return !Objects.equals(existing.content(), entry.content())
            || existing.isOther() != entry.isOther();
    }

    private ProjectApplicationFormPolicy buildPolicy(
        ProjectApplicationForm applicationForm,
        Long sectionId,
        ApplicationFormSectionEntry entry
    ) {
        if (entry.type() == FormSectionType.COMMON) {
            return ProjectApplicationFormPolicy.createCommon(applicationForm, sectionId);
        }
        return ProjectApplicationFormPolicy.createForParts(applicationForm, sectionId, nullSafeAllowedParts(entry));
    }

    private Set<com.umc.product.common.domain.enums.ChallengerPart> nullSafeAllowedParts(
        ApplicationFormSectionEntry entry
    ) {
        return entry.allowedParts() == null ? Set.of() : entry.allowedParts();
    }

    /* =====================================================
     * 5) 응답 조립
     * ===================================================== */

    private ApplicationFormInfo assembleResponse(ProjectApplicationForm applicationForm) {
        FormWithStructureInfo formStructure = getFormUseCase.getFormWithStructure(applicationForm.getFormId());
        List<ProjectApplicationFormPolicy> policies =
            loadPolicyPort.listByApplicationFormId(applicationForm.getId());
        return ApplicationFormInfo.of(applicationForm, formStructure, policies);
    }
}
