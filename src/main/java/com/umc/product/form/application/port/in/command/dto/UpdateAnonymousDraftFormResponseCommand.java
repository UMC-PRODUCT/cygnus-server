package com.umc.product.form.application.port.in.command.dto;

import java.util.List;

import lombok.Builder;

/**
 * 익명 draft 응답의 답변을 전체 교체하는 Command. (익명 임시저장 용도)
 * <p>
 * {@code answers} 는 전체 교체 — 기존 답변은 삭제 후 {@code answers} 로 재구성.
 * <p>
 * (익명 전용) {@code responseAccessKey} 는 발급 시 서버가 반환한 raw 값이며,
 * 서버는 sha256 매칭으로 draft 를 찾는다. 매칭 실패, DRAFT 상태 아님, 기명 draft 인 경우 모두 FORBIDDEN.
 * null 이면 RESPONSE_ACCESS_KEY_REQUIRED 예외.
 */
@Builder
public record UpdateAnonymousDraftFormResponseCommand(
    String responseAccessKey,
    List<AnswerCommand> answers
) {
}
