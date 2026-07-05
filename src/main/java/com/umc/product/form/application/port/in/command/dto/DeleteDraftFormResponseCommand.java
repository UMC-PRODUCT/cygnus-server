package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * draft 응답을 삭제하는 Command. 연관 Answer / AnswerChoice 도 cascade 삭제.
 * <p>
 * SUBMITTED 응답은 이 Command 로 삭제 불가 — SUBMITTED 응답 삭제는 {@code cancelResponse} 사용.
 * 권한 검증은 draft 소유자 본인 여부.
 */
@Builder
public record DeleteDraftFormResponseCommand(
    Long formResponseId,
    Long requesterMemberId
) {
}
