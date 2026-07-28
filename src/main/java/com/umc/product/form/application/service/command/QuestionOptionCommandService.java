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

import com.umc.product.form.application.port.in.command.ManageQuestionOptionUseCase;
import com.umc.product.form.application.port.in.command.dto.CreateQuestionOptionCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteQuestionOptionCommand;
import com.umc.product.form.application.port.in.command.dto.ReorderQuestionOptionsCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateQuestionOptionCommand;
import com.umc.product.form.application.port.out.LoadFormSectionPort;
import com.umc.product.form.application.port.out.LoadQuestionOptionPort;
import com.umc.product.form.application.port.out.LoadQuestionPort;
import com.umc.product.form.application.port.out.SaveQuestionOptionPort;
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

    @Override
    public Long createOption(CreateQuestionOptionCommand command) {
        Question question = loadQuestionPort.findById(command.questionId())
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));

        long nextOrderNo = loadQuestionOptionPort.listByQuestionId(command.questionId()).stream()
            .mapToLong(QuestionOption::getOrderNo)
            .max()
            .orElse(0L) + 1L;

        if (command.nextSectionId() != null) {
            validateNextSection(command.nextSectionId(), question);
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
    public void updateOption(UpdateQuestionOptionCommand command) {
        QuestionOption option = loadQuestionOptionPort.findById(command.optionId())
            .orElseThrow(() -> new FormDomainException(FormErrorCode.FORM_NOT_FOUND));

        boolean clearNextSectionId = Boolean.TRUE.equals(command.clearNextSectionId());
        if (command.nextSectionId() != null) {
            validateNextSection(command.nextSectionId(), option.getQuestion());
        }

        option.update(command.content(), command.isOther(), command.nextSectionId(), clearNextSectionId);
        saveQuestionOptionPort.save(option);
    }

    @Override
    public void deleteOption(DeleteQuestionOptionCommand command) {
        saveQuestionOptionPort.deleteById(command.optionId());
    }

    @Override
    public void reorderOptions(ReorderQuestionOptionsCommand command) {
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

    private void validateNextSection(Long nextSectionId, Question question) {
        validateNextSectionAllowed(question);
        validateNextSectionNotSelfLoop(nextSectionId, question);
        FormSection targetSection = loadTargetSectionForForm(nextSectionId, question);
        validateNextSectionForward(targetSection, question);
        validateOneBranchingQuestionPerSection(question);
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

    private FormSection loadTargetSectionForForm(Long nextSectionId, Question question) {
        FormSection section = loadFormSectionPort.findById(nextSectionId)
            .orElseThrow(() -> new FormDomainException(FormErrorCode.INVALID_VOTE_FORM_STRUCTURE,
                "존재하지 않는 섹션입니다."));
        Long questionFormId = question.getFormSection().getForm().getId();
        if (!section.getForm().getId().equals(questionFormId)) {
            throw new FormDomainException(FormErrorCode.INVALID_VOTE_FORM_STRUCTURE,
                "nextSectionId는 동일한 폼의 섹션이어야 합니다.");
        }
        return section;
    }

    /**
     * 섹션 이동 그래프는 forward-only DAG 로 강제한다.
     * 대상 섹션의 orderNo 가 현재 섹션 orderNo 보다 크지 않으면 (뒤로 가거나 같은 위치) 저장 시 거부한다.
     * 이 규칙으로 back-edge, cycle, 같은 orderNo 간 점프가 모두 차단되어 런타임 사이클 탐지가 불필요하다.
     */
    private static void validateNextSectionForward(FormSection targetSection, Question question) {
        FormSection currentSection = question.getFormSection();
        if (currentSection == null || currentSection.getOrderNo() == null || targetSection.getOrderNo() == null) {
            return;
        }
        if (targetSection.getOrderNo() <= currentSection.getOrderNo()) {
            throw new FormDomainException(FormErrorCode.INVALID_NEXT_SECTION_BACKWARD);
        }
    }

    /**
     * 한 섹션에는 조건부 이동을 지정한 질문(RADIO/DROPDOWN 중 nextSectionId 가 있는 선택지를 가진 질문)이 하나만 존재할 수 있다.
     * 현재 질문 자신은 예외 — 같은 질문에 여러 선택지가 각기 다른 nextSectionId 를 갖는 것은 허용.
     */
    private void validateOneBranchingQuestionPerSection(Question question) {
        FormSection currentSection = question.getFormSection();
        if (currentSection == null || currentSection.getId() == null) {
            return;
        }
        List<Question> siblingBranchCandidates = loadQuestionPort.listBySectionId(currentSection.getId()).stream()
            .filter(q -> !Objects.equals(q.getId(), question.getId()))
            .filter(q -> q.getType() == QuestionType.RADIO || q.getType() == QuestionType.DROPDOWN)
            .toList();
        if (siblingBranchCandidates.isEmpty()) {
            return;
        }
        Set<Long> siblingIds = siblingBranchCandidates.stream()
            .map(Question::getId)
            .collect(Collectors.toSet());
        boolean siblingHasBranching = loadQuestionOptionPort.listByQuestionIdIn(siblingIds).stream()
            .anyMatch(opt -> opt.getNextSectionId() != null);
        if (siblingHasBranching) {
            throw new FormDomainException(FormErrorCode.MULTIPLE_BRANCHING_QUESTIONS_IN_SECTION);
        }
    }
}
