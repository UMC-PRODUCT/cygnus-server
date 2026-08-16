package com.umc.product.recruiting.application.service.command;

import org.springframework.stereotype.Component;

import com.umc.product.form.application.port.in.command.ManageFormSectionUseCase;
import com.umc.product.form.application.port.in.command.ManageFormUseCase;
import com.umc.product.form.application.port.in.command.ManageQuestionUseCase;
import com.umc.product.form.application.port.in.command.dto.CreateDraftFormCommand;
import com.umc.product.form.application.port.in.command.dto.CreateFormSectionCommand;
import com.umc.product.form.application.port.in.command.dto.CreateQuestionCommand;
import com.umc.product.form.application.port.in.command.dto.PublishFormCommand;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.recruiting.domain.RecruitingRound;

import lombok.RequiredArgsConstructor;

/**
 * 면접 일정 조율 Form을 생성하고 게시한다.
 * <p>
 * 지원서 Form과 달리 응답자를 식별해야 배정 보드에서 지원자별 가능 시간을 표시할 수 있으므로 비익명으로 만든다.
 * 구조는 {@code validateAvailabilityFormForOpen}이 요구하는 형태(SCHEDULE 필수 질문 정확히 1개)로 고정한다.
 * <p>
 * 운영진이 문구를 편집하는 UI가 없어 제목과 질문 문구는 상수로 둔다.
 * TODO: 문구 입력 UI가 제공되면 Round 설정에서 받아 넘기도록 확장한다.
 */
@Component
@RequiredArgsConstructor
public class RecruitingInterviewAvailabilityFormProvisioner {

    private static final String FORM_TITLE_SUFFIX = " 면접 일정 조율";
    private static final String SECTION_TITLE = "면접 가능 시간";
    private static final String QUESTION_TITLE = "면접 가능한 시간을 선택해주세요";

    private final ManageFormUseCase manageFormUseCase;
    private final ManageFormSectionUseCase manageFormSectionUseCase;
    private final ManageQuestionUseCase manageQuestionUseCase;

    public AvailabilityFormMapping provision(RecruitingRound round, Long requesterMemberId) {
        Long formId = manageFormUseCase.createDraft(CreateDraftFormCommand.builder()
            .createdMemberId(requesterMemberId)
            .title(round.getTitle() + FORM_TITLE_SUFFIX)
            .isAnonymous(false)
            .allowDuplicateResponses(false)
            .build());

        Long sectionId = manageFormSectionUseCase.createSection(CreateFormSectionCommand.builder()
            .formId(formId)
            .requesterMemberId(requesterMemberId)
            .title(SECTION_TITLE)
            .build());

        Long questionId = manageQuestionUseCase.createQuestion(CreateQuestionCommand.builder()
            .sectionId(sectionId)
            .requesterMemberId(requesterMemberId)
            .type(QuestionType.SCHEDULE)
            .title(QUESTION_TITLE)
            .isRequired(true)
            .build());

        manageFormUseCase.publishForm(PublishFormCommand.builder()
            .formId(formId)
            .requesterMemberId(requesterMemberId)
            .build());

        return new AvailabilityFormMapping(formId, questionId);
    }

    public record AvailabilityFormMapping(Long formId, Long scheduleQuestionId) {
    }
}
