package com.umc.product.form.application.service.command;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.command.ManageQuestionUseCase;
import com.umc.product.form.application.port.in.command.dto.CreateQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.ForkQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.ReorderQuestionsCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateQuestionCommand;
import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.application.port.out.SaveAnswerPort;
import com.umc.product.form.application.port.out.SaveQuestionOptionPort;
import com.umc.product.form.application.port.out.SaveQuestionPort;
import com.umc.product.form.application.service.FormAnswerAttachmentUsageService;
import com.umc.product.form.application.service.FormOwnershipAccessService;
import com.umc.product.form.domain.FormOperation;
import com.umc.product.form.domain.FormOwnerReference;
import com.umc.product.form.domain.FormSection;
import com.umc.product.form.domain.Question;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.form.domain.exception.FormDomainException;
import com.umc.product.form.domain.exception.FormErrorCode;
import com.umc.product.global.exception.constant.Domain;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class QuestionCommandService implements ManageQuestionUseCase {

    private final LoadFormSectionPort loadFormSectionPort;
    private final LoadQuestionPort loadQuestionPort;
    private final SaveQuestionPort saveQuestionPort;
    private final SaveQuestionOptionPort saveQuestionOptionPort;
    private final SaveAnswerPort saveAnswerPort;
    private final FormOwnershipAccessService ownershipAccessService;
    private final FormAnswerAttachmentUsageService attachmentUsageService;

    @Override
    public Long createQuestion(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        CreateQuestionCommand command
    ) {
        FormSection section = loadFormSectionPort.findById(command.sectionId())
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));
        requireStructureAccess(section.getForm().getId(), expectedOwner, actorContext);

        long nextOrderNo = loadQuestionPort.listBySectionId(command.sectionId()).stream()
            .mapToLong(Question::getOrderNo)
            .max()
            .orElse(0L) + 1L;

        Question question = Question.create(
            command.title(),
            command.description(),
            command.type(),
            command.isRequired(),
            nextOrderNo
        );
        question.assignTo(section);
        return saveQuestionPort.save(question).getId();
    }

    @Override
    public void updateQuestion(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        UpdateQuestionCommand command
    ) {
        Question question = loadQuestionPort.findById(command.questionId())
            .orElseThrow(() -> new FormDomainException(FormErrorCode.QUESTION_NOT_FOUND));
        requireStructureAccess(rootFormId(question), expectedOwner, actorContext);

        // type 변경이 있으면 옵션 정리 정책 적용
        if (command.type() != null && command.type() != question.getType()) {
            applyTypeChange(question, command.type());
        }

        // 나머지 속성 PATCH
        question.update(
            command.title(),
            command.description(),
            command.isRequired(),
            Boolean.TRUE.equals(command.clearDescription())
        );

        saveQuestionPort.save(question);
    }

    @Override
    public void deleteQuestion(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        DeleteQuestionCommand command
    ) {
        Long questionId = command.questionId();
        Question question = loadQuestionPort.findById(questionId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.QUESTION_NOT_FOUND));
        requireStructureAccess(rootFormId(question), expectedOwner, actorContext);

        // FK 의존성 거꾸로: AnswerChoice/Answer -> QuestionOption -> Question
        // (Answer cascade 는 SaveAnswerPort.deleteByQuestionId 가 내부적으로 AnswerChoice -> Answer 순으로 처리)
        attachmentUsageService.detachByQuestionId(questionId);
        saveAnswerPort.deleteByQuestionId(questionId);
        saveQuestionOptionPort.deleteAllByQuestionId(questionId);
        saveQuestionPort.deleteById(questionId);
    }

    @Audited(
        domain = Domain.FORM,
        action = AuditAction.REORDER,
        targetType = "Question",
        targetId = "#command.sectionId()",
        description = "'설문 질문 순서를 변경했습니다.'"
    )
    @Override
    public void reorderQuestions(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        ReorderQuestionsCommand command
    ) {
        FormSection section = loadFormSectionPort.findById(command.sectionId())
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));
        requireStructureAccess(section.getForm().getId(), expectedOwner, actorContext);
        List<Question> questions = loadQuestionPort.listBySectionId(command.sectionId());

        Set<Long> existingIds = questions.stream()
            .map(Question::getId)
            .collect(Collectors.toSet());
        Set<Long> requestedIds = new HashSet<>(command.orderedQuestionIds());

        if (!existingIds.equals(requestedIds)) {
            throw new FormDomainException(
                FormErrorCode.INVALID_VOTE_FORM_STRUCTURE,
                "재배치 요청의 질문 ID 셋이 실제 섹션의 질문 ID 셋과 일치하지 않습니다."
            );
        }

        Map<Long, Question> byId = questions.stream()
            .collect(Collectors.toMap(Question::getId, Function.identity()));

        for (int i = 0; i < command.orderedQuestionIds().size(); i++) {
            byId.get(command.orderedQuestionIds().get(i)).updateOrderNo(i + 1);
        }

        saveQuestionPort.saveAll(questions);
    }

    @Override
    public void deactivateQuestion(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        Long questionId
    ) {
        Question question = loadQuestionPort.findById(questionId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.QUESTION_NOT_FOUND));
        requireStructureAccess(rootFormId(question), expectedOwner, actorContext);
        question.deactivate();
        saveQuestionPort.save(question);
    }

    @Override
    public Long forkQuestion(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        ForkQuestionCommand command
    ) {
        Question origin = loadQuestionPort.findById(command.originQuestionId())
            .orElseThrow(() -> new FormDomainException(FormErrorCode.QUESTION_NOT_FOUND));
        requireStructureAccess(rootFormId(origin), expectedOwner, actorContext);

        Question forked = Question.fork(origin);
        forked.assignTo(origin.getFormSection());
        saveQuestionPort.save(forked);

        origin.deactivate();
        saveQuestionPort.save(origin);

        return forked.getId();
    }

    /**
     * type 변경 시 옵션 cascade 정리.
     * <p>
     * 객관식 (RADIO/CHECKBOX/DROPDOWN) -> 비객관식으로 바뀌면 기존 옵션이 의미 없어지므로 삭제.
     * 그 외 조합은 정리할 옵션 자체가 없거나 (비객관식 → *) 그대로 유지 (객관식 <-> 객관식).
     * <p>
     * TODO: 응답(Answer) 무효화 합의 시 분기 추가 (일단 응답 보존).
     */
    private void applyTypeChange(Question question, QuestionType newType) {
        if (isObjective(question.getType()) && !isObjective(newType)) {
            saveQuestionOptionPort.deleteAllByQuestionId(question.getId());
        }
        // TODO: 응답(Answer) 무효화 합의 시 분기 추가

        question.changeType(newType);
    }

    private static boolean isObjective(QuestionType type) {
        return type == QuestionType.RADIO
            || type == QuestionType.CHECKBOX
            || type == QuestionType.DROPDOWN;
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
