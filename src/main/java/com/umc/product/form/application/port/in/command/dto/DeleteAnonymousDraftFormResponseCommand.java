package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 익명 draft 응답을 삭제하는 Command. 연관 Answer / AnswerChoice 도 cascade 삭제.
 * <p>
 * SUBMITTED 응답은 이 Command 로 삭제 불가.
 * <p>
 * (익명 전용) {@code responseAccessKey} 는 발급 시 서버가 반환한 raw 값이며, 서버는 sha256 매칭으로 draft 를 찾는다.
 * 매칭 실패, DRAFT 상태 아님, 기명 draft 인 경우 모두 FORBIDDEN. null 이면 RESPONSE_ACCESS_KEY_REQUIRED 예외.
 */
@Builder
public record DeleteAnonymousDraftFormResponseCommand(
    String responseAccessKey
) {
}
