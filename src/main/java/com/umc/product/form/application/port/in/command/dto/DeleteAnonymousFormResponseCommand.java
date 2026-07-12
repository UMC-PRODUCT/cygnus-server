package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * SUBMITTED 상태인 익명 응답을 삭제하는 Command. (FormResponse + 연관 Answer 모두 삭제)
 * DRAFT 상태 익명 응답 삭제는 {@code DeleteAnonymousDraftFormResponseCommand} 사용.
 * <p>
 * (익명 전용) {@code responseAccessKey} 는 발급 시 서버가 반환한 raw 값이며, 서버는 sha256 매칭으로 응답을 찾는다.
 * 매칭 실패, SUBMITTED 상태 아님, 기명 응답인 경우 모두 FORBIDDEN. null 이면 RESPONSE_ACCESS_KEY_REQUIRED 예외.
 */
@Builder
public record DeleteAnonymousFormResponseCommand(
    String responseAccessKey
) {
}
