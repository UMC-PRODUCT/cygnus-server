package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * SUBMITTED 상태인 익명 응답을 삭제하는 Command. (FormResponse + 연관 Answer 모두 삭제)
 * DRAFT 상태 익명 응답 삭제는 {@code DeleteAnonymousDraftFormResponseCommand} 사용.
 * <p>
 * 익명 response credential은 별도 actor context로 전달된다. 서버는 sha256 매칭으로 응답을 찾으며,
 * 매칭 실패, SUBMITTED 상태 아님, 기명 응답인 경우 모두 FORBIDDEN이다. credential 부재는
 * RESPONSE_ACCESS_KEY_REQUIRED 예외다.
 */
@Builder
public record DeleteAnonymousFormResponseCommand() {
}
