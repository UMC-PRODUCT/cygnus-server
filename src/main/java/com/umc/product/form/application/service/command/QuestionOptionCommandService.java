package com.umc.product.form.application.service.command;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.command.ManageQuestionOptionUseCase;
import com.umc.product.form.application.port.in.command.dto.CreateQuestionOptionCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteQuestionOptionCommand;
import com.umc.product.form.application.port.in.command.dto.ReorderQuestionOptionsCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateQuestionOptionCommand;
import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.application.port.out.LoadQuestionOptionPort;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.application.port.out.SaveQuestionOptionPort;
import com.umc.product.form.application.service.FormOwnershipAccessService;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.QuestionOption;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class QuestionOptionCommandService implements ManageQuestionOptionUseCase {

    private final LoadFormSectionPort loadFormSectionPort;
    private final LoadQuestionPort loadQuestionPort;
    private final LoadQuestionOptionPort loadQuestionOptionPort;
    private final SaveQuestionOptionPort saveQuestionOptionPort;
    private final FormOwnershipAccessService ownershipAccessService;

    @Override
    public Long createOption(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        CreateQuestionOptionCommand command
    ) {
        Question question = loadQuestionPort.findById(command.questionId())
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));
        requireStructureAccess(rootFormId(question), expectedOwner, actorContext);

        long nextOrderNo = loadQuestionOptionPort.listByQuestionId(command.questionId()).stream()
            .mapToLong(QuestionOption::getOrderNo)
            .max()
            .orElse(0L) + 1L;

        if (command.nextSectionId() != null) {
            validateNextSectionAllowed(question);
            validateNextSectionNotSelfLoop(command.nextSectionId(), question);
            validateNextSectionBelongsToForm(command.nextSectionId(), question);
        }

        QuestionOption option = QuestionOption.create(
            command.content(),
            nextOrderNo,
            command.isOther(),
            command.nextSectionId()
        );
        option.assignTo(question);

        return saveQuestionOptionPort.save(option).getId();
    }

    @Override
    public void updateOption(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        UpdateQuestionOptionCommand command
    ) {
        QuestionOption option = loadQuestionOptionPort.findById(command.optionId())
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));
        requireStructureAccess(rootFormId(option.getQuestion()), expectedOwner, actorContext);

        boolean clearNextSectionId = Boolean.TRUE.equals(command.clearNextSectionId());
        if (command.nextSectionId() != null) {
            validateNextSectionAllowed(option.getQuestion());
            validateNextSectionNotSelfLoop(command.nextSectionId(), option.getQuestion());
            validateNextSectionBelongsToForm(command.nextSectionId(), option.getQuestion());
        }

        option.update(command.content(), command.isOther(), command.nextSectionId(), clearNextSectionId);
        saveQuestionOptionPort.save(option);
    }

    @Override
    public void deleteOption(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        DeleteQuestionOptionCommand command
    ) {
        QuestionOption option = loadQuestionOptionPort.findById(command.optionId())
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));
        requireStructureAccess(rootFormId(option.getQuestion()), expectedOwner, actorContext);
        saveQuestionOptionPort.deleteById(command.optionId());
    }

    @Override
    public void reorderOptions(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        ReorderQuestionOptionsCommand command
    ) {
        Question question = loadQuestionPort.findById(command.questionId())
            .orElseThrow(() -> new FormDomainException(FormErrorCode.QUESTION_NOT_FOUND));
        requireStructureAccess(rootFormId(question), expectedOwner, actorContext);
        List<QuestionOption> options = loadQuestionOptionPort.listByQuestionId(command.questionId());

        Set<Long> existingIds = options.stream()
            .map(QuestionOption::getId)
            .collect(Collectors.toSet());
        Set<Long> requestedIds = new HashSet<>(command.orderedOptionIds());

        if (!existingIds.equals(requestedIds)) {
            throw new FormDomainException(
                FormErrorCode.INVALID_VOTE_FORM_STRUCTURE,
                "재배치 요청의 선택지 ID 셋이 실제 질문의 선택지 ID 셋과 일치하지 않습니다."
            );
        }

        Map<Long, QuestionOption> byId = options.stream()
            .collect(Collectors.toMap(QuestionOption::getId, Function.identity()));

        for (int i = 0; i < command.orderedOptionIds().size(); i++) {
            byId.get(command.orderedOptionIds().get(i)).updateOrderNo(i + 1);
        }

        saveQuestionOptionPort.saveAll(options);
    }

    private static void validateNextSectionAllowed(Question question) {
        if (question.getType() != QuestionType.RADIO && question.getType() != QuestionType.DROPDOWN) {
            throw new FormDomainException(FormErrorCode.INVALID_VOTE_FORM_STRUCTURE,
                "조건부 섹션 이동은 RADIO, DROPDOWN 타입 질문에만 지정할 수 있습니다.");
        }
    }

    private static void validateNextSectionNotSelfLoop(Long nextSectionId, Question question) {
        FormSection currentSection = question.getFormSection();
        if (currentSection != null && Objects.equals(nextSectionId, currentSection.getId())) {
            throw new FormDomainException(FormErrorCode.INVALID_NEXT_SECTION_SELF_LOOP);
        }
    }

    private void validateNextSectionBelongsToForm(Long nextSectionId, Question question) {
        FormSection section = loadFormSectionPort.findById(nextSectionId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.INVALID_VOTE_FORM_STRUCTURE,
                "존재하지 않는 섹션입니다."));
        Long questionFormId = question.getFormSection().getForm().getId();
        if (!section.getForm().getId().equals(questionFormId)) {
            throw new FormDomainException(FormErrorCode.INVALID_VOTE_FORM_STRUCTURE,
                "nextSectionId는 동일한 폼의 섹션이어야 합니다.");
        }
    }

    private void requireStructureAccess(
        Long formId,
        FormOwnerReference expectedOwner,
        FormActorContext actorContext
    ) {
        ownershipAccessService.requireMutation(
            formId, expectedOwner, actorContext, FormOperation.MANAGE_STRUCTURE
        );
    }

    private static Long rootFormId(Question question) {
        return question.getFormSection().getForm().getId();
    }
}
