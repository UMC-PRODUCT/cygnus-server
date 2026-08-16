package com.umc.product.form.application.port.in.command.dto;

import java.util.List;

import lombok.Builder;

/**
 * draft 응답의 답변을 전체 교체하는 Command. (임시저장 용도)
 * <p>
 * {@code answers} 는 전체 교체 — 기존 답변은 삭제 후 {@code answers} 로 재구성.
 * draft 가 아닌 응답(SUBMITTED)을 이 Command 로 업데이트하면 예외 — SUBMITTED 수정은 {@code UpdateFormResponseCommand} 사용.
 * <p>
 * (기명 전용) {@code requesterMemberId} 는 권한 검증용 — draft 소유자 본인만 가능.
 * 소유자와 다르거나, draft 가 익명이거나, null 이면 FORM_RESPONSE_FORBIDDEN 예외.
 */
@Builder
public record UpdateDraftFormResponseCommand(
    Long formResponseId,
    Long requesterMemberId,
    List<AnswerCommand> answers
) {
}
