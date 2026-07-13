package com.umc.product.form.application.port.in.command.dto;

import java.time.Instant;
import java.util.List;

import lombok.Builder;

/**
 * 익명 DRAFT FormResponse 에 개별 답변을 추가하는 Command.
 * <p>
 * {@code responseAccessKey} 는 발급 시 서버가 반환한 raw 값이며, 서버는 sha256 매칭으로 draft 를 찾는다.
 * 매칭 실패, DRAFT 상태 아님, 기명 draft 인 경우 모두 FORBIDDEN. null 이면 RESPONSE_ACCESS_KEY_REQUIRED.
 * <p>
 * 타입별 필드 사용 규칙은 {@link CreateAnswerCommand} 참고.
 */
@Builder
public record CreateAnonymousAnswerCommand(
    String responseAccessKey,
    Long questionId,
    String textValue,
    List<Long> selectedOptionIds,
    List<String> fileIds,
    List<Instant> times
) {
}
