package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * draft 응답을 삭제하는 Command. 연관 Answer / AnswerChoice 도 cascade 삭제.
 * <p>
 * SUBMITTED 응답은 이 Command 로 삭제 불가 — SUBMITTED 응답 삭제는 {@code cancelResponse} 사용.
 * <p>
 * 기명 actor는 별도 context로 전달되며 draft 소유자 본인만 가능하다.
 * 소유자와 다르거나 draft가 익명이거나 인증 actor가 없으면 FORM_RESPONSE_FORBIDDEN 예외.
 */
@Builder
public record DeleteDraftFormResponseCommand(
    Long formResponseId
) {
}
