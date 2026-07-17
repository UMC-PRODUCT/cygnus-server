package com.umc.product.form.application.port.in.command.dto;

import java.util.List;

import lombok.Builder;

/**
 * 이미 SUBMITTED 상태인 익명 응답의 답변을 전체 교체하는 Command (재제출 의미).
 * <p>
 * 결과 status 는 SUBMITTED 그대로 유지되므로 제출 무결성을 위해 필수 답변 누락 검증을 수행한다.
 * 작성 중인 익명 응답을 갱신하려는 경우는 {@code UpdateAnonymousDraftFormResponseCommand} 사용.
 * <p>
 * 익명 response credential은 별도 actor context로 전달된다. 서버는 sha256 매칭으로 응답을 찾으며,
 * 매칭 실패, SUBMITTED 상태 아님, 기명 응답인 경우 모두 FORBIDDEN이다. credential 부재는
 * RESPONSE_ACCESS_KEY_REQUIRED 예외다.
 */
@Builder
public record UpdateAnonymousFormResponseCommand(
    List<AnswerCommand> answers
) {
}
