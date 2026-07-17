package com.umc.product.form.application.service.query;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.umc.product.form.domain.Form;
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

    @Override
    public Optional<FormInfo> findById(Long formId) {
        return loadFormPort.findById(formId)
            .map(FormInfo::from);
    }

    @Override
    public FormInfo getById(Long formId) {
        return loadFormPort.findById(formId)
            .map(FormInfo::from)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));
    }

    @Override
    public Map<Long, FormInfo> batchGetByIds(Collection<Long> formIds) {
        if (formIds == null || formIds.isEmpty()) {
            return Map.of();
        }

        List<Long> uniqueFormIds = formIds.stream()
            .distinct()
            .toList();

        Map<Long, FormInfo> formsById = loadFormPort.batchGetByIds(uniqueFormIds).stream()
            .collect(Collectors.toMap(
                Form::getId,
                FormInfo::from,
                (left, right) -> left
            ));

        return uniqueFormIds.stream()
            .collect(Collectors.toMap(
                formId -> formId,
                formsById::get,
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    @Override
    public FormWithStructureInfo getFormWithStructure(Long formId) {
        Form form = loadFormPort.findById(formId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));

        List<FormSection> sections = loadFormSectionPort.listByFormId(formId);
        Set<Long> sectionIds = sections.stream()
            .map(FormSection::getId)
            .collect(Collectors.toSet());

        List<Question> questions = loadQuestionPort.listBySectionIdIn(sectionIds);
        Set<Long> questionIds = questions.stream()
            .map(Question::getId)
            .collect(Collectors.toSet());

        List<QuestionOption> options = loadQuestionOptionPort.listByQuestionIdIn(questionIds);

        return buildFormInfo(form, sections, questions, options);
    }

    @Override
    public Map<Long, FormWithStructureInfo> batchGetFormsWithStructure(Collection<Long> formIds) {
        if (formIds == null || formIds.isEmpty()) {
            return Map.of();
        }

        List<Long> uniqueFormIds = formIds.stream()
            .collect(Collectors.collectingAndThen(
                Collectors.toCollection(LinkedHashSet::new),
                List::copyOf
            ));
        if (uniqueFormIds.isEmpty()) {
            return Map.of();
        }

        List<Form> forms = loadFormPort.batchGetByIds(uniqueFormIds);
        List<FormSection> sections = loadFormSectionPort.listByFormIds(uniqueFormIds);
        Set<Long> sectionIds = sections.stream()
            .map(FormSection::getId)
            .collect(Collectors.toSet());

        List<Question> questions = loadQuestionPort.listBySectionIdIn(sectionIds);
        Set<Long> questionIds = questions.stream()
            .map(Question::getId)
            .collect(Collectors.toSet());

        List<QuestionOption> options = loadQuestionOptionPort.listByQuestionIdIn(questionIds);
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
    public FormWithStructureInfo getFormWithStructureByQuestionIds(Long formId, Set<Long> questionIds) {
        Form form = loadFormPort.findById(formId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));

        if (questionIds.isEmpty()) {
            List<FormSection> sections = loadFormSectionPort.listByFormId(formId);
            return buildFormInfo(form, sections, List.of(), List.of());
        }

        // Answer.questionId 기반으로 직접 조회 (isActive 무관 — fork된 구 버전 포함)
        List<Question> questions = loadQuestionPort.listByIdIn(questionIds);
        Set<Long> resolvedQuestionIds = questions.stream()
            .map(Question::getId)
            .collect(Collectors.toSet());

        List<QuestionOption> options = loadQuestionOptionPort.listByQuestionIdIn(resolvedQuestionIds);

        // 질문이 속한 섹션만 추려서 조회
        Set<Long> sectionIds = questions.stream()
            .map(q -> q.getFormSection().getId())
            .collect(Collectors.toSet());
        List<FormSection> sections = loadFormSectionPort.listByFormId(formId).stream()
            .filter(s -> sectionIds.contains(s.getId()))
            .toList();

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
}
