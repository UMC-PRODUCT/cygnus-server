package com.umc.product.feedback.application.service.command;

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

import com.umc.product.feedback.application.port.in.command.ManageUserFeedbackTemplateUseCase;
import com.umc.product.feedback.application.port.in.command.dto.CreateUserFeedbackTemplateCommand;
import com.umc.product.feedback.application.port.in.command.dto.FeedbackTemplateQuestionEntry;
import com.umc.product.feedback.application.port.in.command.dto.FeedbackTemplateQuestionOptionEntry;
import com.umc.product.feedback.application.port.in.command.dto.FeedbackTemplateSectionEntry;
import com.umc.product.feedback.application.port.in.command.dto.UpdateUserFeedbackTemplateCommand;
import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateDetailInfo;
import com.umc.product.feedback.application.port.out.LoadUserFeedbackTemplatePort;
import com.umc.product.feedback.application.port.out.SaveUserFeedbackTemplatePort;
import com.umc.product.feedback.domain.UserFeedbackTemplate;
import com.umc.product.feedback.domain.exception.FeedbackDomainException;
import com.umc.product.feedback.domain.exception.FeedbackErrorCode;
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
import com.umc.product.form.application.port.in.command.dto.PublishFormCommand;
import com.umc.product.form.application.port.in.command.dto.ReorderFormSectionsCommand;
import com.umc.product.form.application.port.in.command.dto.ReorderQuestionOptionsCommand;
import com.umc.product.form.application.port.in.command.dto.ReorderQuestionsCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateFormCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateFormSectionCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateQuestionOptionCommand;
import com.umc.product.form.application.port.in.query.GetFormResponseUseCase;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo.Option;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo.QuestionWithOptions;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo.SectionWithQuestions;
import com.umc.product.form.domain.enums.QuestionType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserFeedbackTemplateCommandService implements ManageUserFeedbackTemplateUseCase {

    private static final Set<QuestionType> CHOICE_QUESTION_TYPES =
        Set.of(QuestionType.RADIO, QuestionType.CHECKBOX, QuestionType.DROPDOWN);

    private final LoadUserFeedbackTemplatePort loadTemplatePort;
    private final SaveUserFeedbackTemplatePort saveTemplatePort;
    private final ManageFormUseCase manageFormUseCase;
    private final ManageFormSectionUseCase manageFormSectionUseCase;
    private final ManageQuestionUseCase manageQuestionUseCase;
    private final ManageQuestionOptionUseCase manageQuestionOptionUseCase;
    private final GetFormUseCase getFormUseCase;
    private final GetFormResponseUseCase getFormResponseUseCase;

    @Override
    public UserFeedbackTemplateDetailInfo create(CreateUserFeedbackTemplateCommand command) {
        validateOptionTypeRules(command.sections());
        validateNoActiveDuplicate(command);

        Long formId = manageFormUseCase.createDraft(CreateDraftFormCommand.builder()
            .createdMemberId(command.requesterMemberId())
            .title(command.title())
            .description(command.description())
            .isAnonymous(command.isAnonymous())
            .allowDuplicateResponses(command.allowDuplicateResponses())
            .build());

        applyNewStructure(formId, command.requesterMemberId(), command.sections());
        manageFormUseCase.publishForm(PublishFormCommand.builder()
            .formId(formId)
            .requesterMemberId(command.requesterMemberId())
            .build());

        UserFeedbackTemplate template = saveTemplatePort.save(UserFeedbackTemplate.create(
            command.context(),
            command.targetType(),
            formId
        ));
        return assembleResponse(template);
    }

    @Override
    public UserFeedbackTemplateDetailInfo update(UpdateUserFeedbackTemplateCommand command) {
        validateOptionTypeRules(command.sections());
        validateNoActiveDuplicate(command);

        UserFeedbackTemplate template = loadTemplatePort.getById(command.templateId());
        FormWithStructureInfo existing = getFormUseCase.getFormWithStructure(template.getFormId());
        boolean hasResponses = !getFormResponseUseCase.listByFormId(template.getFormId()).isEmpty();

        Map<Long, SectionWithQuestions> existingSectionById = existing.sections().stream()
            .collect(Collectors.toMap(SectionWithQuestions::sectionId, Function.identity()));
        validateIds(command.sections(), existingSectionById);
        if (hasResponses) {
            validateNoDestructiveChanges(command.sections(), existing.sections());
        }

        template.updateContextAndTarget(command.context(), command.targetType());
        saveTemplatePort.save(template);

        manageFormUseCase.updateForm(UpdateFormCommand.builder()
            .formId(template.getFormId())
            .requesterMemberId(command.requesterMemberId())
            .title(command.title())
            .description(command.description())
            .clearDescription(command.description() == null || Boolean.TRUE.equals(command.clearDescription()))
            .isAnonymous(command.isAnonymous())
            .allowDuplicateResponses(command.allowDuplicateResponses())
            .build());

        applyDiff(template.getFormId(), existing, command.requesterMemberId(), command.sections(), hasResponses);
        return assembleResponse(template);
    }

    @Override
    public void delete(Long templateId) {
        UserFeedbackTemplate template = loadTemplatePort.getById(templateId);
        if (!template.isActive()) {
            return;
        }
        template.deactivate();
        saveTemplatePort.save(template);
    }

    private void validateNoActiveDuplicate(CreateUserFeedbackTemplateCommand command) {
        if (loadTemplatePort.existsActiveByContextAndTargetType(command.context(), command.targetType())) {
            throw new FeedbackDomainException(FeedbackErrorCode.USER_FEEDBACK_TEMPLATE_ALREADY_ACTIVE);
        }
    }

    private void validateNoActiveDuplicate(UpdateUserFeedbackTemplateCommand command) {
        if (loadTemplatePort.existsActiveByContextAndTargetTypeExcludingId(
            command.context(),
            command.targetType(),
            command.templateId()
        )) {
            throw new FeedbackDomainException(FeedbackErrorCode.USER_FEEDBACK_TEMPLATE_ALREADY_ACTIVE);
        }
    }

    private void applyNewStructure(
        Long formId,
        Long requesterMemberId,
        List<FeedbackTemplateSectionEntry> sections
    ) {
        List<Long> orderedSectionIds = new ArrayList<>();
        for (FeedbackTemplateSectionEntry section : sections) {
            Long sectionId = createNewSection(formId, requesterMemberId, section);
            orderedSectionIds.add(sectionId);
        }
        reorderSections(formId, requesterMemberId, orderedSectionIds);
    }

    private void applyDiff(
        Long formId,
        FormWithStructureInfo existing,
        Long requesterMemberId,
        List<FeedbackTemplateSectionEntry> sections,
        boolean hasResponses
    ) {
        Map<Long, SectionWithQuestions> existingSectionById = existing.sections().stream()
            .collect(Collectors.toMap(SectionWithQuestions::sectionId, Function.identity()));

        List<Long> orderedSectionIds = new ArrayList<>();
        for (FeedbackTemplateSectionEntry section : sections) {
            Long sectionId = section.sectionId() == null
                ? createNewSection(formId, requesterMemberId, section)
                : updateExistingSection(
                    section,
                    existingSectionById.get(section.sectionId()),
                    requesterMemberId,
                    hasResponses
                );
            orderedSectionIds.add(sectionId);
        }

        if (!hasResponses) {
            deleteRemovedSections(sections, existing.sections(), requesterMemberId);
        }
        reorderSections(formId, requesterMemberId, orderedSectionIds);
    }

    private Long createNewSection(Long formId, Long requesterMemberId, FeedbackTemplateSectionEntry entry) {
        Long sectionId = manageFormSectionUseCase.createSection(CreateFormSectionCommand.builder()
            .formId(formId)
            .requesterMemberId(requesterMemberId)
            .title(entry.title())
            .description(entry.description())
            .build());

        List<Long> orderedQuestionIds = new ArrayList<>();
        for (FeedbackTemplateQuestionEntry question : entry.questions()) {
            orderedQuestionIds.add(createNewQuestion(sectionId, requesterMemberId, question));
        }
        reorderQuestions(sectionId, requesterMemberId, orderedQuestionIds);
        return sectionId;
    }

    private Long updateExistingSection(
        FeedbackTemplateSectionEntry entry,
        SectionWithQuestions existing,
        Long requesterMemberId,
        boolean hasResponses
    ) {
        if (sectionMetaChanged(existing, entry)) {
            manageFormSectionUseCase.updateSection(UpdateFormSectionCommand.builder()
                .sectionId(entry.sectionId())
                .requesterMemberId(requesterMemberId)
                .title(entry.title())
                .description(entry.description())
                .clearDescription(entry.description() == null)
                .build());
        }

        applyQuestionDiff(entry, existing, requesterMemberId, hasResponses);
        return entry.sectionId();
    }

    private void applyQuestionDiff(
        FeedbackTemplateSectionEntry section,
        SectionWithQuestions existing,
        Long requesterMemberId,
        boolean hasResponses
    ) {
        Map<Long, QuestionWithOptions> existingQuestionById = existing.questions().stream()
            .collect(Collectors.toMap(QuestionWithOptions::questionId, Function.identity()));

        List<Long> orderedQuestionIds = new ArrayList<>();
        for (FeedbackTemplateQuestionEntry question : section.questions()) {
            Long questionId = question.questionId() == null
                ? createNewQuestion(section.sectionId(), requesterMemberId, question)
                : updateExistingQuestion(
                    question,
                    existingQuestionById.get(question.questionId()),
                    requesterMemberId,
                    hasResponses
                );
            orderedQuestionIds.add(questionId);
        }

        if (!hasResponses) {
            deleteRemovedQuestions(section.questions(), existing.questions(), requesterMemberId);
        }
        reorderQuestions(section.sectionId(), requesterMemberId, orderedQuestionIds);
    }

    private Long createNewQuestion(Long sectionId, Long requesterMemberId, FeedbackTemplateQuestionEntry entry) {
        Long questionId = manageQuestionUseCase.createQuestion(CreateQuestionCommand.builder()
            .sectionId(sectionId)
            .requesterMemberId(requesterMemberId)
            .type(entry.type())
            .title(entry.title())
            .description(entry.description())
            .isRequired(entry.isRequired())
            .build());

        List<Long> orderedOptionIds = new ArrayList<>();
        for (FeedbackTemplateQuestionOptionEntry option : entry.options()) {
            orderedOptionIds.add(createNewOption(questionId, requesterMemberId, option));
        }
        reorderOptions(questionId, requesterMemberId, orderedOptionIds);
        return questionId;
    }

    private Long updateExistingQuestion(
        FeedbackTemplateQuestionEntry entry,
        QuestionWithOptions existing,
        Long requesterMemberId,
        boolean hasResponses
    ) {
        if (questionMetaChanged(existing, entry)) {
            manageQuestionUseCase.updateQuestion(UpdateQuestionCommand.builder()
                .questionId(entry.questionId())
                .requesterMemberId(requesterMemberId)
                .type(entry.type())
                .title(entry.title())
                .description(entry.description())
                .clearDescription(entry.description() == null)
                .isRequired(entry.isRequired())
                .build());
        }

        if (CHOICE_QUESTION_TYPES.contains(entry.type())) {
            applyOptionDiff(entry, existing, requesterMemberId, hasResponses);
        }
        return entry.questionId();
    }

    private void applyOptionDiff(
        FeedbackTemplateQuestionEntry question,
        QuestionWithOptions existing,
        Long requesterMemberId,
        boolean hasResponses
    ) {
        Map<Long, Option> existingOptionById = existing.options().stream()
            .collect(Collectors.toMap(Option::optionId, Function.identity()));

        List<Long> orderedOptionIds = new ArrayList<>();
        for (FeedbackTemplateQuestionOptionEntry option : question.options()) {
            Long optionId = option.optionId() == null
                ? createNewOption(question.questionId(), requesterMemberId, option)
                : updateExistingOption(option, existingOptionById.get(option.optionId()), requesterMemberId);
            orderedOptionIds.add(optionId);
        }

        if (!hasResponses) {
            deleteRemovedOptions(question.options(), existing.options(), requesterMemberId);
        }
        reorderOptions(question.questionId(), requesterMemberId, orderedOptionIds);
    }

    private Long createNewOption(Long questionId, Long requesterMemberId, FeedbackTemplateQuestionOptionEntry entry) {
        return manageQuestionOptionUseCase.createOption(CreateQuestionOptionCommand.builder()
            .questionId(questionId)
            .requesterMemberId(requesterMemberId)
            .content(entry.content())
            .isOther(entry.isOther())
            .build());
    }

    private Long updateExistingOption(
        FeedbackTemplateQuestionOptionEntry entry,
        Option existing,
        Long requesterMemberId
    ) {
        if (!Objects.equals(existing.content(), entry.content()) || existing.isOther() != entry.isOther()) {
            manageQuestionOptionUseCase.updateOption(UpdateQuestionOptionCommand.builder()
                .optionId(entry.optionId())
                .requesterMemberId(requesterMemberId)
                .content(entry.content())
                .isOther(entry.isOther())
                .build());
        }
        return entry.optionId();
    }

    private void deleteRemovedSections(
        List<FeedbackTemplateSectionEntry> requestSections,
        List<SectionWithQuestions> existingSections,
        Long requesterMemberId
    ) {
        Set<Long> requestedIds = requestSections.stream()
            .map(FeedbackTemplateSectionEntry::sectionId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        for (SectionWithQuestions existing : existingSections) {
            if (!requestedIds.contains(existing.sectionId())) {
                manageFormSectionUseCase.deleteSection(DeleteFormSectionCommand.builder()
                    .sectionId(existing.sectionId())
                    .requesterMemberId(requesterMemberId)
                    .build());
            }
        }
    }

    private void deleteRemovedQuestions(
        List<FeedbackTemplateQuestionEntry> requestQuestions,
        List<QuestionWithOptions> existingQuestions,
        Long requesterMemberId
    ) {
        Set<Long> requestedIds = requestQuestions.stream()
            .map(FeedbackTemplateQuestionEntry::questionId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        for (QuestionWithOptions existing : existingQuestions) {
            if (!requestedIds.contains(existing.questionId())) {
                manageQuestionUseCase.deleteQuestion(DeleteQuestionCommand.builder()
                    .questionId(existing.questionId())
                    .requesterMemberId(requesterMemberId)
                    .build());
            }
        }
    }

    private void deleteRemovedOptions(
        List<FeedbackTemplateQuestionOptionEntry> requestOptions,
        List<Option> existingOptions,
        Long requesterMemberId
    ) {
        Set<Long> requestedIds = requestOptions.stream()
            .map(FeedbackTemplateQuestionOptionEntry::optionId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        for (Option existing : existingOptions) {
            if (!requestedIds.contains(existing.optionId())) {
                manageQuestionOptionUseCase.deleteOption(DeleteQuestionOptionCommand.builder()
                    .optionId(existing.optionId())
                    .requesterMemberId(requesterMemberId)
                    .build());
            }
        }
    }

    private void reorderSections(Long formId, Long requesterMemberId, List<Long> orderedSectionIds) {
        if (orderedSectionIds.isEmpty()) {
            return;
        }
        manageFormSectionUseCase.reorderSections(ReorderFormSectionsCommand.builder()
            .formId(formId)
            .requesterMemberId(requesterMemberId)
            .orderedSectionIds(orderedSectionIds)
            .build());
    }

    private void reorderQuestions(Long sectionId, Long requesterMemberId, List<Long> orderedQuestionIds) {
        if (orderedQuestionIds.isEmpty()) {
            return;
        }
        manageQuestionUseCase.reorderQuestions(ReorderQuestionsCommand.builder()
            .sectionId(sectionId)
            .requesterMemberId(requesterMemberId)
            .orderedQuestionIds(orderedQuestionIds)
            .build());
    }

    private void reorderOptions(Long questionId, Long requesterMemberId, List<Long> orderedOptionIds) {
        if (orderedOptionIds.isEmpty()) {
            return;
        }
        manageQuestionOptionUseCase.reorderOptions(ReorderQuestionOptionsCommand.builder()
            .questionId(questionId)
            .requesterMemberId(requesterMemberId)
            .orderedOptionIds(orderedOptionIds)
            .build());
    }

    private void validateOptionTypeRules(List<FeedbackTemplateSectionEntry> sections) {
        for (FeedbackTemplateSectionEntry section : sections) {
            for (FeedbackTemplateQuestionEntry question : section.questions()) {
                boolean choiceType = CHOICE_QUESTION_TYPES.contains(question.type());
                boolean hasOptions = !question.options().isEmpty();
                if (choiceType && !hasOptions) {
                    throw new FeedbackDomainException(FeedbackErrorCode.FEEDBACK_TEMPLATE_INVALID_FORM_STRUCTURE);
                }
                if (!choiceType && hasOptions) {
                    throw new FeedbackDomainException(FeedbackErrorCode.FEEDBACK_TEMPLATE_INVALID_FORM_STRUCTURE);
                }
            }
        }
    }

    private void validateIds(
        List<FeedbackTemplateSectionEntry> sections,
        Map<Long, SectionWithQuestions> existingSectionById
    ) {
        for (FeedbackTemplateSectionEntry section : sections) {
            if (section.sectionId() == null) {
                ensureAllQuestionAndOptionIdsNull(section.questions());
                continue;
            }
            SectionWithQuestions existingSection = existingSectionById.get(section.sectionId());
            if (existingSection == null) {
                throw new FeedbackDomainException(FeedbackErrorCode.FEEDBACK_TEMPLATE_INVALID_FORM_STRUCTURE);
            }
            validateQuestionAndOptionIds(section.questions(), existingSection);
        }
    }

    private void validateQuestionAndOptionIds(
        List<FeedbackTemplateQuestionEntry> questions,
        SectionWithQuestions existingSection
    ) {
        Map<Long, QuestionWithOptions> existingQuestionById = existingSection.questions().stream()
            .collect(Collectors.toMap(QuestionWithOptions::questionId, Function.identity()));
        for (FeedbackTemplateQuestionEntry question : questions) {
            if (question.questionId() == null) {
                ensureAllOptionIdsNull(question.options());
                continue;
            }
            QuestionWithOptions existingQuestion = existingQuestionById.get(question.questionId());
            if (existingQuestion == null) {
                throw new FeedbackDomainException(FeedbackErrorCode.FEEDBACK_TEMPLATE_INVALID_FORM_STRUCTURE);
            }
            Set<Long> existingOptionIds = existingQuestion.options().stream()
                .map(Option::optionId)
                .collect(Collectors.toSet());
            for (FeedbackTemplateQuestionOptionEntry option : question.options()) {
                if (option.optionId() != null && !existingOptionIds.contains(option.optionId())) {
                    throw new FeedbackDomainException(FeedbackErrorCode.FEEDBACK_TEMPLATE_INVALID_FORM_STRUCTURE);
                }
            }
        }
    }

    private void ensureAllQuestionAndOptionIdsNull(List<FeedbackTemplateQuestionEntry> questions) {
        for (FeedbackTemplateQuestionEntry question : questions) {
            if (question.questionId() != null) {
                throw new FeedbackDomainException(FeedbackErrorCode.FEEDBACK_TEMPLATE_INVALID_FORM_STRUCTURE);
            }
            ensureAllOptionIdsNull(question.options());
        }
    }

    private void ensureAllOptionIdsNull(List<FeedbackTemplateQuestionOptionEntry> options) {
        for (FeedbackTemplateQuestionOptionEntry option : options) {
            if (option.optionId() != null) {
                throw new FeedbackDomainException(FeedbackErrorCode.FEEDBACK_TEMPLATE_INVALID_FORM_STRUCTURE);
            }
        }
    }

    private void validateNoDestructiveChanges(
        List<FeedbackTemplateSectionEntry> requestSections,
        List<SectionWithQuestions> existingSections
    ) {
        Map<Long, FeedbackTemplateSectionEntry> requestSectionById = requestSections.stream()
            .filter(section -> section.sectionId() != null)
            .collect(Collectors.toMap(FeedbackTemplateSectionEntry::sectionId, Function.identity()));
        for (SectionWithQuestions existingSection : existingSections) {
            FeedbackTemplateSectionEntry requestSection = requestSectionById.get(existingSection.sectionId());
            if (requestSection == null) {
                throw destructiveChange();
            }
            validateNoDestructiveQuestionChanges(requestSection.questions(), existingSection.questions());
        }
    }

    private void validateNoDestructiveQuestionChanges(
        List<FeedbackTemplateQuestionEntry> requestQuestions,
        List<QuestionWithOptions> existingQuestions
    ) {
        Map<Long, FeedbackTemplateQuestionEntry> requestQuestionById = requestQuestions.stream()
            .filter(question -> question.questionId() != null)
            .collect(Collectors.toMap(FeedbackTemplateQuestionEntry::questionId, Function.identity()));
        for (QuestionWithOptions existingQuestion : existingQuestions) {
            FeedbackTemplateQuestionEntry requestQuestion = requestQuestionById.get(existingQuestion.questionId());
            if (requestQuestion == null || requestQuestion.type() != existingQuestion.type()) {
                throw destructiveChange();
            }
            validateNoDestructiveOptionChanges(requestQuestion.options(), existingQuestion.options());
        }
    }

    private void validateNoDestructiveOptionChanges(
        List<FeedbackTemplateQuestionOptionEntry> requestOptions,
        List<Option> existingOptions
    ) {
        Set<Long> requestOptionIds = requestOptions.stream()
            .map(FeedbackTemplateQuestionOptionEntry::optionId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
        for (Option existingOption : existingOptions) {
            if (!requestOptionIds.contains(existingOption.optionId())) {
                throw destructiveChange();
            }
        }
    }

    private FeedbackDomainException destructiveChange() {
        return new FeedbackDomainException(FeedbackErrorCode.FEEDBACK_TEMPLATE_DESTRUCTIVE_CHANGE_NOT_ALLOWED);
    }

    private boolean sectionMetaChanged(SectionWithQuestions existing, FeedbackTemplateSectionEntry entry) {
        return !Objects.equals(existing.title(), entry.title())
            || !Objects.equals(existing.description(), entry.description());
    }

    private boolean questionMetaChanged(QuestionWithOptions existing, FeedbackTemplateQuestionEntry entry) {
        return existing.type() != entry.type()
            || !Objects.equals(existing.title(), entry.title())
            || !Objects.equals(existing.description(), entry.description())
            || existing.isRequired() != entry.isRequired();
    }

    private UserFeedbackTemplateDetailInfo assembleResponse(UserFeedbackTemplate template) {
        return UserFeedbackTemplateDetailInfo.of(
            template,
            getFormUseCase.getFormWithStructure(template.getFormId())
        );
    }
}
