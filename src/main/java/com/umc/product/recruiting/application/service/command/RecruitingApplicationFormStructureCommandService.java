package com.umc.product.recruiting.application.service.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.umc.product.recruiting.application.port.in.command.UpsertRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.UpsertRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpsertRecruitingApplicationFormCommand.OptionEntry;
import com.umc.product.recruiting.application.port.in.command.dto.UpsertRecruitingApplicationFormCommand.QuestionEntry;
import com.umc.product.recruiting.application.port.in.command.dto.UpsertRecruitingApplicationFormCommand.SectionEntry;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingFormSectionPolicyPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingRoundPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingApplicationFormPort;
import com.umc.product.recruiting.application.port.out.SaveRecruitingFormSectionPolicyPort;
import com.umc.product.recruiting.domain.RecruitingApplicationForm;
import com.umc.product.recruiting.domain.RecruitingFormSectionPolicy;
import com.umc.product.recruiting.domain.RecruitingRound;
import com.umc.product.recruiting.domain.enums.RecruitingFormSectionType;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RecruitingApplicationFormStructureCommandService implements UpsertRecruitingApplicationFormUseCase {

    private static final Set<QuestionType> CHOICE_TYPES =
        Set.of(QuestionType.RADIO, QuestionType.CHECKBOX, QuestionType.DROPDOWN);

    private final LoadRecruitingRoundPort loadRoundPort;
    private final LoadRecruitingApplicationFormPort loadApplicationFormPort;
    private final SaveRecruitingApplicationFormPort saveApplicationFormPort;
    private final LoadRecruitingFormSectionPolicyPort loadPolicyPort;
    private final SaveRecruitingFormSectionPolicyPort savePolicyPort;
    private final ManageFormUseCase manageFormUseCase;
    private final ManageFormSectionUseCase manageFormSectionUseCase;
    private final ManageQuestionUseCase manageQuestionUseCase;
    private final ManageQuestionOptionUseCase manageQuestionOptionUseCase;
    private final GetFormUseCase getFormUseCase;

    @Override
    public Long upsert(UpsertRecruitingApplicationFormCommand command) {
        RecruitingRound round = loadRoundPort.getById(command.roundId());
        validateRound(round, command.seasonId());
        validateRequest(round, command.sections());

        RecruitingApplicationForm applicationForm;
        FormWithStructureInfo existing;
        var maybeApplicationForm = loadApplicationFormPort.findByRoundId(round.getId());
        if (maybeApplicationForm.isPresent()) {
            applicationForm = maybeApplicationForm.get();
            applicationForm.validateStructureMutable();
            existing = getFormUseCase.getFormWithStructure(applicationForm.getFormId());
            updateFormMeta(applicationForm, round, command);
        } else {
            applicationForm = createForm(round, command);
            existing = FormWithStructureInfo.builder()
                .formId(applicationForm.getFormId())
                .sections(List.of())
                .build();
        }

        applyDiff(applicationForm, existing, command);
        return applicationForm.getId();
    }

    private RecruitingApplicationForm createForm(
        RecruitingRound round,
        UpsertRecruitingApplicationFormCommand command
    ) {
        Long formId = manageFormUseCase.createDraft(CreateDraftFormCommand.builder()
            .createdMemberId(command.requesterMemberId())
            .title(round.getTitle())
            .description(command.description())
            .isAnonymous(true)
            .allowDuplicateResponses(false)
            .build());
        return saveApplicationFormPort.save(RecruitingApplicationForm.create(round, formId));
    }

    private void updateFormMeta(
        RecruitingApplicationForm applicationForm,
        RecruitingRound round,
        UpsertRecruitingApplicationFormCommand command
    ) {
        manageFormUseCase.updateForm(UpdateFormCommand.builder()
            .formId(applicationForm.getFormId())
            .requesterMemberId(command.requesterMemberId())
            .title(round.getTitle())
            .description(command.description())
            .clearDescription(command.description() == null)
            .isAnonymous(true)
            .allowDuplicateResponses(false)
            .build());
    }

    private void applyDiff(
        RecruitingApplicationForm applicationForm,
        FormWithStructureInfo existing,
        UpsertRecruitingApplicationFormCommand command
    ) {
        Map<Long, SectionWithQuestions> existingSections = existing.sections().stream()
            .collect(Collectors.toMap(SectionWithQuestions::sectionId, Function.identity()));
        validateOwnedIds(command.sections(), existingSections);

        Map<Long, RecruitingFormSectionPolicy> policies = loadPolicyPort
            .listByApplicationFormId(applicationForm.getId()).stream()
            .collect(Collectors.toMap(RecruitingFormSectionPolicy::getFormSectionId, Function.identity()));
        Map<String, Long> sectionIdByKey = new java.util.LinkedHashMap<>();
        for (SectionEntry section : command.sections()) {
            Long sectionId = section.sectionId() == null
                ? createSection(applicationForm, section, command.requesterMemberId())
                : updateSection(
                    applicationForm,
                    section,
                    policies.get(section.sectionId()),
                    command.requesterMemberId()
                );
            sectionIdByKey.put(section.clientKey(), sectionId);
        }

        List<Long> orderedSectionIds = new ArrayList<>();
        for (SectionEntry section : command.sections()) {
            Long sectionId = sectionIdByKey.get(section.clientKey());
            orderedSectionIds.add(sectionId);
            syncQuestions(
                sectionId,
                section.questions(),
                existingSections.get(section.sectionId()),
                sectionIdByKey,
                command.requesterMemberId()
            );
        }

        deleteRemovedSections(command.sections(), existing.sections(), command.requesterMemberId());
        manageFormSectionUseCase.reorderSections(ReorderFormSectionsCommand.builder()
            .formId(applicationForm.getFormId())
            .requesterMemberId(command.requesterMemberId())
            .orderedSectionIds(orderedSectionIds)
            .build());
    }

    private Long createSection(
        RecruitingApplicationForm applicationForm,
        SectionEntry entry,
        Long requesterMemberId
    ) {
        Long sectionId = manageFormSectionUseCase.createSection(CreateFormSectionCommand.builder()
            .formId(applicationForm.getFormId())
            .requesterMemberId(requesterMemberId)
            .title(entry.title())
            .description(entry.description())
            .build());
        savePolicyPort.save(newPolicy(applicationForm, sectionId, entry));
        return sectionId;
    }

    private Long updateSection(
        RecruitingApplicationForm applicationForm,
        SectionEntry entry,
        RecruitingFormSectionPolicy policy,
        Long requesterMemberId
    ) {
        manageFormSectionUseCase.updateSection(UpdateFormSectionCommand.builder()
            .sectionId(entry.sectionId())
            .requesterMemberId(requesterMemberId)
            .title(entry.title())
            .description(entry.description())
            .clearDescription(entry.description() == null)
            .build());
        if (policy == null) {
            savePolicyPort.save(newPolicy(applicationForm, entry.sectionId(), entry));
        } else {
            policy.updatePolicy(entry.type(), entry.track());
            savePolicyPort.save(policy);
        }
        return entry.sectionId();
    }

    private RecruitingFormSectionPolicy newPolicy(
        RecruitingApplicationForm applicationForm,
        Long sectionId,
        SectionEntry entry
    ) {
        return entry.type() == RecruitingFormSectionType.COMMON
            ? RecruitingFormSectionPolicy.createCommon(applicationForm, sectionId)
            : RecruitingFormSectionPolicy.createTrack(applicationForm, sectionId, entry.track());
    }

    private void syncQuestions(
        Long sectionId,
        List<QuestionEntry> requestedQuestions,
        SectionWithQuestions existingSection,
        Map<String, Long> sectionIdByKey,
        Long requesterMemberId
    ) {
        List<QuestionWithOptions> existingQuestions = existingSection == null
            ? List.of()
            : existingSection.questions();
        Map<Long, QuestionWithOptions> existingById = existingQuestions.stream()
            .collect(Collectors.toMap(QuestionWithOptions::questionId, Function.identity()));
        List<Long> orderedQuestionIds = new ArrayList<>();
        for (QuestionEntry question : requestedQuestions) {
            Long questionId = question.questionId() == null
                ? createQuestion(sectionId, question, sectionIdByKey, requesterMemberId)
                : updateQuestion(question, existingById.get(question.questionId()), sectionIdByKey, requesterMemberId);
            orderedQuestionIds.add(questionId);
        }
        deleteRemovedQuestions(requestedQuestions, existingQuestions, requesterMemberId);
        if (!orderedQuestionIds.isEmpty()) {
            manageQuestionUseCase.reorderQuestions(ReorderQuestionsCommand.builder()
                .sectionId(sectionId)
                .requesterMemberId(requesterMemberId)
                .orderedQuestionIds(orderedQuestionIds)
                .build());
        }
    }

    private Long createQuestion(
        Long sectionId,
        QuestionEntry entry,
        Map<String, Long> sectionIdByKey,
        Long requesterMemberId
    ) {
        Long questionId = manageQuestionUseCase.createQuestion(CreateQuestionCommand.builder()
            .sectionId(sectionId)
            .requesterMemberId(requesterMemberId)
            .type(entry.type())
            .title(entry.title())
            .description(entry.description())
            .isRequired(entry.required())
            .build());
        syncOptions(questionId, entry.options(), List.of(), sectionIdByKey, requesterMemberId);
        return questionId;
    }

    private Long updateQuestion(
        QuestionEntry entry,
        QuestionWithOptions existing,
        Map<String, Long> sectionIdByKey,
        Long requesterMemberId
    ) {
        manageQuestionUseCase.updateQuestion(UpdateQuestionCommand.builder()
            .questionId(entry.questionId())
            .requesterMemberId(requesterMemberId)
            .type(entry.type())
            .title(entry.title())
            .description(entry.description())
            .clearDescription(entry.description() == null)
            .isRequired(entry.required())
            .build());
        syncOptions(entry.questionId(), entry.options(), existing.options(), sectionIdByKey, requesterMemberId);
        return entry.questionId();
    }

    private void syncOptions(
        Long questionId,
        List<OptionEntry> requestedOptions,
        List<Option> existingOptions,
        Map<String, Long> sectionIdByKey,
        Long requesterMemberId
    ) {
        List<Long> orderedOptionIds = new ArrayList<>();
        for (OptionEntry option : requestedOptions) {
            Long nextSectionId = option.nextSectionKey() == null
                ? null
                : sectionIdByKey.get(option.nextSectionKey());
            Long optionId;
            if (option.optionId() == null) {
                optionId = manageQuestionOptionUseCase.createOption(CreateQuestionOptionCommand.builder()
                    .questionId(questionId)
                    .requesterMemberId(requesterMemberId)
                    .content(option.content())
                    .isOther(option.other())
                    .nextSectionId(nextSectionId)
                    .build());
            } else {
                manageQuestionOptionUseCase.updateOption(UpdateQuestionOptionCommand.builder()
                    .optionId(option.optionId())
                    .requesterMemberId(requesterMemberId)
                    .content(option.content())
                    .isOther(option.other())
                    .nextSectionId(nextSectionId)
                    .clearNextSectionId(nextSectionId == null)
                    .build());
                optionId = option.optionId();
            }
            orderedOptionIds.add(optionId);
        }
        deleteRemovedOptions(requestedOptions, existingOptions, requesterMemberId);
        if (!orderedOptionIds.isEmpty()) {
            manageQuestionOptionUseCase.reorderOptions(ReorderQuestionOptionsCommand.builder()
                .questionId(questionId)
                .requesterMemberId(requesterMemberId)
                .orderedOptionIds(orderedOptionIds)
                .build());
        }
    }

    private void deleteRemovedSections(
        List<SectionEntry> requested,
        List<SectionWithQuestions> existing,
        Long requesterMemberId
    ) {
        Set<Long> retainedIds = requested.stream()
            .map(SectionEntry::sectionId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        for (SectionWithQuestions section : existing) {
            if (retainedIds.contains(section.sectionId())) {
                continue;
            }
            manageFormSectionUseCase.deleteSection(DeleteFormSectionCommand.builder()
                .sectionId(section.sectionId())
                .requesterMemberId(requesterMemberId)
                .build());
            savePolicyPort.deleteByFormSectionId(section.sectionId());
        }
    }

    private void deleteRemovedQuestions(
        List<QuestionEntry> requested,
        List<QuestionWithOptions> existing,
        Long requesterMemberId
    ) {
        Set<Long> retainedIds = requested.stream()
            .map(QuestionEntry::questionId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        for (QuestionWithOptions question : existing) {
            if (!retainedIds.contains(question.questionId())) {
                manageQuestionUseCase.deleteQuestion(DeleteQuestionCommand.builder()
                    .questionId(question.questionId())
                    .requesterMemberId(requesterMemberId)
                    .build());
            }
        }
    }

    private void deleteRemovedOptions(
        List<OptionEntry> requested,
        List<Option> existing,
        Long requesterMemberId
    ) {
        Set<Long> retainedIds = requested.stream()
            .map(OptionEntry::optionId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        for (Option option : existing) {
            if (!retainedIds.contains(option.optionId())) {
                manageQuestionOptionUseCase.deleteOption(DeleteQuestionOptionCommand.builder()
                    .optionId(option.optionId())
                    .requesterMemberId(requesterMemberId)
                    .build());
            }
        }
    }

    private void validateRound(RecruitingRound round, Long seasonId) {
        if (!Objects.equals(round.getSeason().getId(), seasonId)) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_ROUND_NOT_FOUND);
        }
        if (round.getStatus() != RecruitingRoundStatus.DRAFT) {
            throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_INVALID_TRANSITION);
        }
    }

    private void validateRequest(RecruitingRound round, List<SectionEntry> sections) {
        if (sections == null || sections.isEmpty()) {
            throw invalidForm();
        }
        Map<String, SectionEntry> sectionByKey;
        try {
            sectionByKey = sections.stream().collect(Collectors.toMap(SectionEntry::clientKey, Function.identity()));
        } catch (RuntimeException exception) {
            throw invalidForm();
        }
        if (sectionByKey.containsKey(null) || sectionByKey.keySet().stream().anyMatch(String::isBlank)) {
            throw invalidForm();
        }
        for (SectionEntry section : sections) {
            validateSectionPolicy(round, section);
            for (QuestionEntry question : section.questions()) {
                boolean choice = CHOICE_TYPES.contains(question.type());
                if (choice == question.options().isEmpty()) {
                    throw invalidForm();
                }
                for (OptionEntry option : question.options()) {
                    if (option.nextSectionKey() == null) {
                        continue;
                    }
                    SectionEntry target = sectionByKey.get(option.nextSectionKey());
                    if (target == null) {
                        throw invalidForm();
                    }
                    if (section.type() == RecruitingFormSectionType.TRACK
                        && target.type() != RecruitingFormSectionType.COMMON
                        && target.track() != section.track()) {
                        throw invalidForm();
                    }
                }
            }
        }
    }

    private void validateSectionPolicy(RecruitingRound round, SectionEntry section) {
        if (section.type() == RecruitingFormSectionType.COMMON && section.track() == null) {
            return;
        }
        if (section.type() == RecruitingFormSectionType.TRACK && round.isRecruitableTrack(section.track())) {
            return;
        }
        throw new RecruitingDomainException(RecruitingErrorCode.RECRUITING_FORM_SECTION_POLICY_INVALID_TRACK);
    }

    private void validateOwnedIds(
        List<SectionEntry> requested,
        Map<Long, SectionWithQuestions> existingSections
    ) {
        for (SectionEntry section : requested) {
            if (section.sectionId() == null) {
                ensureNewQuestions(section.questions());
                continue;
            }
            SectionWithQuestions existingSection = existingSections.get(section.sectionId());
            if (existingSection == null) {
                throw invalidForm();
            }
            Map<Long, QuestionWithOptions> existingQuestions = existingSection.questions().stream()
                .collect(Collectors.toMap(QuestionWithOptions::questionId, Function.identity()));
            for (QuestionEntry question : section.questions()) {
                if (question.questionId() == null) {
                    ensureNewOptions(question.options());
                    continue;
                }
                QuestionWithOptions existingQuestion = existingQuestions.get(question.questionId());
                if (existingQuestion == null) {
                    throw invalidForm();
                }
                Set<Long> optionIds = existingQuestion.options().stream()
                    .map(Option::optionId)
                    .collect(Collectors.toSet());
                if (question.options().stream()
                    .map(OptionEntry::optionId)
                    .filter(Objects::nonNull)
                    .anyMatch(id -> !optionIds.contains(id))) {
                    throw invalidForm();
                }
            }
        }
    }

    private void ensureNewQuestions(List<QuestionEntry> questions) {
        if (questions.stream().anyMatch(question -> question.questionId() != null)) {
            throw invalidForm();
        }
        questions.forEach(question -> ensureNewOptions(question.options()));
    }

    private void ensureNewOptions(List<OptionEntry> options) {
        if (options.stream().anyMatch(option -> option.optionId() != null)) {
            throw invalidForm();
        }
    }

    private RecruitingDomainException invalidForm() {
        return new RecruitingDomainException(RecruitingErrorCode.RECRUITING_APPLICATION_FORM_INVALID);
    }
}
