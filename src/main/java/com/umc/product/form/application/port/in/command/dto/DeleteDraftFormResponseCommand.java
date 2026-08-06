package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * draft 응답을 삭제하는 Command. 연관 Answer / AnswerChoice 도 cascade 삭제.
 * <p>
 * SUBMITTED 응답은 이 Command 로 삭제 불가 — SUBMITTED 응답 삭제는 {@code cancelResponse} 사용.
 * <p>
 * (기명 전용) {@code requesterMemberId} 는 권한 검증용 — draft 소유자 본인만 가능.
 * 소유자와 다르거나, draft 가 익명이거나, null 이면 FORM_RESPONSE_FORBIDDEN 예외.
 */
@Builder
public record DeleteDraftFormResponseCommand(
    Long formResponseId,
    Long requesterMemberId
) {
}
