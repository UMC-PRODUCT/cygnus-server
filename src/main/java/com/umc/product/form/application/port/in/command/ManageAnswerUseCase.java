package com.umc.product.form.application.port.in.command;

import com.umc.product.form.application.port.in.FormActorContext;
import com.umc.product.form.application.port.in.command.dto.CreateAnonymousAnswerCommand;
import com.umc.product.form.application.port.in.command.dto.CreateAnswerCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteAnonymousAnswerCommand;
import com.umc.product.form.application.port.in.command.dto.DeleteAnswerCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateAnonymousAnswerCommand;
import com.umc.product.form.application.port.in.command.dto.UpdateAnswerCommand;
import com.umc.product.form.domain.FormOwnerReference;

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
 * {@link FormActorContext#authenticatedMemberId()} 기반 소유자 검증. 익명 메서드({@link #createAnonymousAnswer} /
 * {@link #updateAnonymousAnswer} / {@link #deleteAnonymousAnswer})는
 * {@link FormActorContext#responseAccessKey()} 기반 sha256 매칭. 두 경계는 서로 우회 불가.
 */
public interface ManageAnswerUseCase {

    /**
     * (기명 전용) DRAFT FormResponse 에 개별 답변을 추가한다.
     * <p>
     * 같은 질문에 대한 답변이 이미 있으면 예외 — 수정은 {@link #updateAnswer} 사용.
     * FormResponse 가 DRAFT 가 아니면 NOT_DRAFT. 익명 draft / 소유자 불일치 / 인증 actor 부재면 FORBIDDEN.
     *
     * @return 생성된 Answer ID
     */
    Long createAnswer(FormOwnerReference expectedOwner, FormActorContext actorContext, CreateAnswerCommand command);

    /**
     * (기명 전용) 개별 답변을 전체 교체한다. (textValue / selectedOptionIds / fileIds / times 등)
     * 기존 AnswerChoice 는 전부 삭제 후 재생성.
     * 답변 ID 가 없으면 ANSWER_NOT_FOUND. FormResponse 가 DRAFT 가 아니면 NOT_DRAFT.
     * 익명 draft / 소유자 불일치 / 인증 actor 부재면 FORBIDDEN.
     */
    void updateAnswer(FormOwnerReference expectedOwner, FormActorContext actorContext, UpdateAnswerCommand command);

    /**
     * (기명 전용) 개별 답변을 삭제한다. 연관 AnswerChoice 도 cascade 삭제.
     * 답변 ID 가 없으면 ANSWER_NOT_FOUND. FormResponse 가 DRAFT 가 아니면 NOT_DRAFT.
     * 익명 draft / 소유자 불일치 / 인증 actor 부재면 FORBIDDEN.
     */
    void deleteAnswer(FormOwnerReference expectedOwner, FormActorContext actorContext, DeleteAnswerCommand command);

    /**
     * (익명 전용) 익명 DRAFT FormResponse 에 개별 답변을 추가한다.
     * <p>
     * actor context의 response credential을 sha256 매칭해 익명 draft를 로드한다.
     * 매칭 실패, DRAFT 아님, 기명 draft인 경우 모두 FORBIDDEN이며 credential 부재는 RESPONSE_ACCESS_KEY_REQUIRED다.
     * 같은 질문에 대한 답변이 이미 있으면 {@link #updateAnonymousAnswer} 사용.
     *
     * @return 생성된 Answer ID
     */
    Long createAnonymousAnswer(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        CreateAnonymousAnswerCommand command
    );

    /**
     * (익명 전용) 익명 DRAFT FormResponse 의 개별 답변을 전체 교체한다.
     * <p>
     * {@code answerId} 로 답변 로드 → 그 응답의 저장된 hash와 actor context의 response credential을 sha256 매칭한다.
     * 익명 경계 유출 방지를 위해 credential 부재를 제외한 모든 실패 케이스(Answer 없음, DRAFT 아님,
     * 기명 draft, hash 불일치)는 FORBIDDEN으로 통일한다.
     */
    void updateAnonymousAnswer(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        UpdateAnonymousAnswerCommand command
    );

    /**
     * (익명 전용) 익명 DRAFT FormResponse 의 개별 답변을 삭제한다. 연관 AnswerChoice 도 cascade 삭제.
     * <p>
     * 검증 규칙은 {@link #updateAnonymousAnswer} 와 동일.
     */
    void deleteAnonymousAnswer(
        FormOwnerReference expectedOwner,
        FormActorContext actorContext,
        DeleteAnonymousAnswerCommand command
    );
}
