package com.umc.product.form.application.port.in.command;

import com.umc.product.form.application.port.in.command.dto.CreateAnonymousAnswerCommand;
import com.umc.product.form.application.port.in.command.dto.CreateAnswerCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteAnonymousAnswerCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteAnswerCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateAnonymousAnswerCommand;
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
 * <b>기명/익명 분리</b>: 기명 메서드({@link #createAnswer} / {@link #updateAnswer} / {@link #deleteAnswer})는
 * {@code requesterMemberId} 기반 소유자 검증. 익명 메서드({@link #createAnonymousAnswer} / {@link #updateAnonymousAnswer}
 * / {@link #deleteAnonymousAnswer})는 {@code responseAccessKey} 기반 sha256 매칭. 두 경계는 서로 우회 불가.
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

    /**
     * (익명 전용) 익명 DRAFT FormResponse 에 개별 답변을 추가한다.
     * <p>
     * {@code responseAccessKey} sha256 매칭으로 익명 draft 로드. 매칭 실패, DRAFT 아님, 기명 draft 인 경우 모두 FORBIDDEN.
     * {@code responseAccessKey} 가 null 이면 RESPONSE_ACCESS_KEY_REQUIRED.
     * 같은 질문에 대한 답변이 이미 있으면 {@link #updateAnonymousAnswer} 사용.
     *
     * @return 생성된 Answer ID
     */
    Long createAnonymousAnswer(CreateAnonymousAnswerCommand command);

    /**
     * (익명 전용) 익명 DRAFT FormResponse 의 개별 답변을 전체 교체한다.
     * <p>
     * {@code answerId} 로 답변 로드 → 그 응답의 저장된 hash 와 {@code responseAccessKey} sha256 매칭.
     * 익명 경계 유출 방지를 위해 rawKey null 을 제외한 모든 실패 케이스 (Answer 없음, DRAFT 아님,
     * 기명 draft, hash 불일치) 는 FORBIDDEN 으로 통일. {@code responseAccessKey} 가 null 이면
     * RESPONSE_ACCESS_KEY_REQUIRED.
     */
    void updateAnonymousAnswer(UpdateAnonymousAnswerCommand command);

    /**
     * (익명 전용) 익명 DRAFT FormResponse 의 개별 답변을 삭제한다. 연관 AnswerChoice 도 cascade 삭제.
     * <p>
     * 검증 규칙은 {@link #updateAnonymousAnswer} 와 동일.
     */
    void deleteAnonymousAnswer(DeleteAnonymousAnswerCommand command);
}
