package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 익명 draft 응답을 삭제하는 Command. 연관 Answer / AnswerChoice 도 cascade 삭제.
 * <p>
 * SUBMITTED 응답은 이 Command 로 삭제 불가.
 * <p>
 * 익명 response credential은 별도 actor context로 전달된다. 서버는 sha256 매칭으로 draft를 찾으며,
 * 매칭 실패, DRAFT 상태 아님, 기명 draft인 경우 모두 FORBIDDEN이다. credential 부재는
 * RESPONSE_ACCESS_KEY_REQUIRED 예외다.
 */
@Builder
public record DeleteAnonymousDraftFormResponseCommand() {
}
