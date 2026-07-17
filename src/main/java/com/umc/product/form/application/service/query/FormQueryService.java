package com.umc.product.form.application.service.query;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.query.GetFormUseCase;
import com.umc.product.form.application.port.in.query.dto.FormInfo;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo.Option;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo.QuestionWithOptions;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo.SectionWithQuestions;
import com.umc.product.form.application.port.out.LoadFormPort;
import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.application.port.out.LoadQuestionOptionPort;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.application.service.FormOwnershipAccessService;
import com.umc.product.form.domain.Form;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FormQueryService implements GetFormUseCase {

    private final LoadFormPort loadFormPort;
    private final LoadFormSectionPort loadFormSectionPort;
    private final LoadQuestionPort loadQuestionPort;
    private final LoadQuestionOptionPort loadQuestionOptionPort;
    private final FormOwnershipAccessService ownershipAccessService;

    @Override
    public Optional<FormInfo> findById(FormOwnerReference expectedOwner, FormActorContext actorContext) {
        requireRead(expectedOwner, actorContext);
        Optional<Form> form = loadFormPort.findById(expectedOwner.formId());
        form.ifPresent(value -> requireSameRoot(value.getId(), expectedOwner));
        return form
            .map(FormInfo::from);
    }

    @Override
    public FormInfo getById(FormOwnerReference expectedOwner, FormActorContext actorContext) {
        requireRead(expectedOwner, actorContext);
        Form form = loadFormPort.findById(expectedOwner.formId())
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));
        requireSameRoot(form.getId(), expectedOwner);
        return FormInfo.from(form);
    }

    @Override
    public FormWithStructureInfo getFormWithStructure(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext
    ) {
        requireRead(expectedOwner, actorContext);
        Long formId = expectedOwner.formId();
        Form form = loadFormPort.findById(formId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));
        requireSameRoot(form.getId(), expectedOwner);

        List<FormSection> sections = loadFormSectionPort.listByFormId(formId);
        sections.forEach(section -> requireSameRoot(
            section.getForm().getId(), expectedOwner
        ));
        Set<Long> sectionIds = sections.stream()
            .map(FormSection::getId)
            .collect(Collectors.toSet());

        List<Question> questions = loadQuestionPort.listBySectionIdIn(sectionIds);
        questions.forEach(question -> {
            requireInScope(question.getFormSection().getId(), sectionIds);
            requireSameRoot(
                question.getFormSection().getForm().getId(), expectedOwner
            );
        });
        Set<Long> questionIds = questions.stream()
            .map(Question::getId)
            .collect(Collectors.toSet());

        List<QuestionOption> options = loadQuestionOptionPort.listByQuestionIdIn(questionIds);
        options.forEach(option -> {
            requireInScope(option.getQuestion().getId(), questionIds);
            requireSameRoot(
                option.getQuestion().getFormSection().getForm().getId(), expectedOwner
            );
        });

        return buildFormInfo(form, sections, questions, options);
    }

    @Override
    public Map<Long, FormWithStructureInfo> batchGetFormsWithStructure(
        Collection<FormOwnerReference> expectedOwners,
        FormActorContext actorContext
    ) {
        FormOwnerScope ownerScope = FormOwnerScope.from(expectedOwners);
        if (ownerScope.isEmpty()) {
            return Map.of();
        }

        ownerScope.owners().forEach(owner -> requireRead(owner, actorContext));
        List<Long> uniqueFormIds = ownerScope.formIds();

        List<Form> forms = loadFormPort.batchGetByIds(uniqueFormIds);
        forms.forEach(form -> ownerScope.require(form.getId()));
        List<FormSection> sections = loadFormSectionPort.listByFormIds(uniqueFormIds);
        sections.forEach(section -> ownerScope.require(section.getForm().getId()));
        Set<Long> sectionIds = sections.stream()
            .map(FormSection::getId)
            .collect(Collectors.toSet());

        List<Question> questions = loadQuestionPort.listBySectionIdIn(sectionIds);
        questions.forEach(question -> {
            requireInScope(question.getFormSection().getId(), sectionIds);
            ownerScope.require(question.getFormSection().getForm().getId());
        });
        Set<Long> questionIds = questions.stream()
            .map(Question::getId)
            .collect(Collectors.toSet());

        List<QuestionOption> options = loadQuestionOptionPort.listByQuestionIdIn(questionIds);
        options.forEach(option -> {
            requireInScope(option.getQuestion().getId(), questionIds);
            ownerScope.require(option.getQuestion().getFormSection().getForm().getId());
        });
        Map<Long, List<FormSection>> sectionsByFormId = sections.stream()
            .collect(Collectors.groupingBy(section -> section.getForm().getId()));
        Map<Long, List<Question>> questionsBySection = questions.stream()
            .collect(Collectors.groupingBy(question -> question.getFormSection().getId()));
        Map<Long, List<QuestionOption>> optionsByQuestion = options.stream()
            .collect(Collectors.groupingBy(option -> option.getQuestion().getId()));

        return forms.stream()
            .collect(Collectors.toMap(
                Form::getId,
                form -> buildFormInfo(
                    form,
                    sectionsByFormId.getOrDefault(form.getId(), List.of()),
                    questionsBySection,
                    optionsByQuestion
                ),
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    @Override
    public FormWithStructureInfo getFormWithStructureByQuestionIds(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Set<Long> questionIds
    ) {
        requireRead(expectedOwner, actorContext);
        Long formId = expectedOwner.formId();
        Form form = loadFormPort.findById(formId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));
        requireSameRoot(form.getId(), expectedOwner);

        if (questionIds.isEmpty()) {
            List<FormSection> sections = loadFormSectionPort.listByFormId(formId);
            sections.forEach(section -> requireSameRoot(
                section.getForm().getId(), expectedOwner
            ));
            return buildFormInfo(form, sections, List.of(), List.of());
        }

        // Answer.questionId 기반으로 직접 조회 (isActive 무관 — fork된 구 버전 포함)
        List<Question> questions = loadQuestionPort.listByIdIn(questionIds);
        questions.forEach(question -> {
            requireInScope(question.getId(), questionIds);
            requireSameRoot(
                question.getFormSection().getForm().getId(), expectedOwner
            );
        });
        Set<Long> resolvedQuestionIds = questions.stream()
            .map(Question::getId)
            .collect(Collectors.toSet());

        List<QuestionOption> options = loadQuestionOptionPort.listByQuestionIdIn(resolvedQuestionIds);
        options.forEach(option -> {
            requireInScope(option.getQuestion().getId(), resolvedQuestionIds);
            requireSameRoot(
                option.getQuestion().getFormSection().getForm().getId(), expectedOwner
            );
        });

        // 질문이 속한 섹션만 추려서 조회
        Set<Long> sectionIds = questions.stream()
            .map(q -> q.getFormSection().getId())
            .collect(Collectors.toSet());
        List<FormSection> sections = loadFormSectionPort.listByFormId(formId).stream()
            .filter(s -> sectionIds.contains(s.getId()))
            .toList();
        sections.forEach(section -> requireSameRoot(
            section.getForm().getId(), expectedOwner
        ));

        return buildFormInfo(form, sections, questions, options);
    }

    private FormWithStructureInfo buildFormInfo(
        Form form,
        List<FormSection> sections,
        List<Question> questions,
        List<QuestionOption> options
    ) {
        Map<Long, List<Question>> questionsBySection = questions.stream()
            .collect(Collectors.groupingBy(q -> q.getFormSection().getId()));
        Map<Long, List<QuestionOption>> optionsByQuestion = options.stream()
            .collect(Collectors.groupingBy(o -> o.getQuestion().getId()));

        return buildFormInfo(form, sections, questionsBySection, optionsByQuestion);
    }

    private FormWithStructureInfo buildFormInfo(
        Form form,
        List<FormSection> sections,
        Map<Long, List<Question>> questionsBySection,
        Map<Long, List<QuestionOption>> optionsByQuestion
    ) {
        List<SectionWithQuestions> sectionDtos = sections.stream()
            .map(section -> SectionWithQuestions.builder()
                .sectionId(section.getId())
                .title(section.getTitle())
                .description(section.getDescription())
                .orderNo(section.getOrderNo())
                .questions(toQuestionDtos(
                    questionsBySection.getOrDefault(section.getId(), List.of()),
                    optionsByQuestion
                ))
                .build())
            .toList();

        return FormWithStructureInfo.builder()
            .formId(form.getId())
            .createdMemberId(form.getCreatedMemberId())
            .title(form.getTitle())
            .description(form.getDescription())
            .status(form.getStatus())
            .isAnonymous(form.isAnonymous())
            .allowDuplicateResponses(form.isAllowDuplicateResponses())
            .createdAt(form.getCreatedAt())
            .updatedAt(form.getUpdatedAt())
            .sections(sectionDtos)
            .build();
    }

    private List<QuestionWithOptions> toQuestionDtos(
        List<Question> questions,
        Map<Long, List<QuestionOption>> optionsByQuestion
    ) {
        return questions.stream()
            .map(question -> QuestionWithOptions.builder()
                .questionId(question.getId())
                .title(question.getTitle())
                .description(question.getDescription())
                .type(question.getType())
                .isRequired(Boolean.TRUE.equals(question.getIsRequired()))
                .orderNo(question.getOrderNo())
                .options(toOptionDtos(
                    optionsByQuestion.getOrDefault(question.getId(), List.of())
                ))
                .build())
            .toList();
    }

    private List<Option> toOptionDtos(List<QuestionOption> options) {
        return options.stream()
            .map(option -> Option.builder()
                .optionId(option.getId())
                .content(option.getContent())
                .orderNo(option.getOrderNo())
                .isOther(option.isOther())
                .build())
            .toList();
    }

    private void requireRead(FormOwnerReference expectedOwner, FormActorContext actorContext) {
        Long formId = expectedOwner == null ? null : expectedOwner.formId();
        requireRead(formId, expectedOwner, actorContext);
    }

    private void requireRead(
        Long formId,
        FormOwnerReference expectedOwner,
        FormActorContext actorContext
    ) {
        ownershipAccessService.requireRead(
            formId, expectedOwner, actorContext, FormOperation.READ
        );
    }

    private static void requireSameRoot(
        Long resolvedFormId,
        FormOwnerReference expectedOwner
    ) {
        if (resolvedFormId == null
            || expectedOwner == null
            || !resolvedFormId.equals(expectedOwner.formId())) {
            throw new FormDomainException(FormErrorCode.FORM_OWNERSHIP_FORBIDDEN);
        }
    }

    private static void requireInScope(Long resolvedId, Set<Long> expectedIds) {
        if (resolvedId == null || expectedIds == null || !expectedIds.contains(resolvedId)) {
            throw new FormDomainException(FormErrorCode.FORM_OWNERSHIP_FORBIDDEN);
        }
    }
}
