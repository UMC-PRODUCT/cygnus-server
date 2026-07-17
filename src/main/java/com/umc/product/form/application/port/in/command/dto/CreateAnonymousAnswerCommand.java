package com.umc.product.form.application.port.in.command.dto;

import java.time.Instant;
import java.util.List;

import lombok.Builder;

/**
 * 익명 DRAFT FormResponse 에 개별 답변을 추가하는 Command.
 * <p>
 * 익명 response credential은 별도 actor context로 전달된다. 서버는 sha256 매칭으로 draft를 찾으며,
 * 매칭 실패, DRAFT 상태 아님, 기명 draft인 경우 모두 FORBIDDEN이다. credential 부재는
 * RESPONSE_ACCESS_KEY_REQUIRED다.
 * <p>
 * 타입별 필드 사용 규칙은 {@link CreateAnswerCommand} 참고.
 */
@Builder
public record CreateAnonymousAnswerCommand(
    Long questionId,
    String textValue,
    List<Long> selectedOptionIds,
    List<String> fileIds,
    List<Instant> times
) {
}
