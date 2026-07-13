package com.umc.product.form.application.port.in.command;

import com.umc.product.form.application.port.in.command.dto.CreateAnswerCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteAnswerCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateAnswerCommand;

/**
 * Answer(개별 답변) 관리 UseCase.
 * <p>
 * 답변은 FormResponse 의 하위로, 하나의 질문에 대한 사용자의 응답 단위다.
 * {@code ManageFormResponseUseCase.updateDraft} 가 FormResponse 내 모든 답변을 전체 교체하는 반면, 이 UseCase 는 개별 답변 단위 조작
 * <p>
 * 대상 FormResponse 는 DRAFT 상태여야 하며, SUBMITTED 응답의 답변은 이 UseCase 로 조작 불가
 * (SUBMITTED 응답 수정은 {@code ManageFormResponseUseCase.updateResponse} 사용).
 * <p>
 * AnswerChoice (객관식 선택지)는 Answer 내부에 흡수되어 함께 관리된다 — {@code CreateAnswerCommand.selectedOptionIds} 로 선택지 지정.
 * <p>
 * 이 UseCase 의 create/update/delete 는 <b>기명 전용</b>이다. 익명 응답의 답변은 조작할 수 없으며 요청 시 FORBIDDEN 처리.
 * (익명 응답의 답변 조작은 별도 UseCase)
 */
public interface ManageAnswerUseCase {

    /**
     * (기명 전용) DRAFT FormResponse 에 개별 답변을 추가한다.
     * <p>
     * 같은 질문에 대한 답변이 이미 있으면 예외 — 수정은 {@link #updateAnswer} 사용.
     * FormResponse 가 DRAFT 가 아니면 NOT_DRAFT. 익명 draft / 소유자 불일치 / requesterMemberId null 이면 FORBIDDEN.
     *
     * @return 생성된 Answer ID
     */
    Long createAnswer(CreateAnswerCommand command);

    /**
     * (기명 전용) 개별 답변을 전체 교체한다. (textValue / selectedOptionIds / fileIds / times 등)
     * 기존 AnswerChoice 는 전부 삭제 후 재생성.
     * 답변 ID 가 없으면 ANSWER_NOT_FOUND. FormResponse 가 DRAFT 가 아니면 NOT_DRAFT.
     * 익명 draft / 소유자 불일치 / requesterMemberId null 이면 FORBIDDEN.
     */
    void updateAnswer(UpdateAnswerCommand command);

    /**
     * (기명 전용) 개별 답변을 삭제한다. 연관 AnswerChoice 도 cascade 삭제.
     * 답변 ID 가 없으면 ANSWER_NOT_FOUND. FormResponse 가 DRAFT 가 아니면 NOT_DRAFT.
     * 익명 draft / 소유자 불일치 / requesterMemberId null 이면 FORBIDDEN.
     */
    void deleteAnswer(DeleteAnswerCommand command);
}
