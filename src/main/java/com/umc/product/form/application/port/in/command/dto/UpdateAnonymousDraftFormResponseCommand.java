package com.umc.product.form.application.port.in.command.dto;

import java.util.List;

import lombok.Builder;

/**
 * 익명 draft 응답의 답변을 전체 교체하는 Command. (익명 임시저장 용도)
 * <p>
 * {@code answers} 는 전체 교체 — 기존 답변은 삭제 후 {@code answers} 로 재구성.
 * <p>
 * 익명 response credential은 별도 actor context로 전달된다. 서버는 sha256 매칭으로 draft를 찾으며,
 * 매칭 실패, DRAFT 상태 아님, 기명 draft인 경우 모두 FORBIDDEN이다. credential 부재는
 * RESPONSE_ACCESS_KEY_REQUIRED 예외다.
 */
@Builder
public record UpdateAnonymousDraftFormResponseCommand(
    List<AnswerCommand> answers
) {
}
