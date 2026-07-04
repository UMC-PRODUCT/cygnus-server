package com.umc.product.form.application.port.in.command.dto;

import java.time.Instant;
import java.util.List;
import lombok.Builder;

/**
 * 개별 답변을 전체 교체하는 Command.
 * <p>
 * 기존 답변 값(textValue / AnswerChoice / fileIds / times)을 모두 삭제 후 새 값으로 재구성.
 * 파일 필드 사용 규칙은 {@code CreateAnswerCommand} 참고.
 */
@Builder
public record UpdateAnswerCommand(
    Long answerId,
    Long requesterMemberId,
    String textValue,
    List<Long> selectedOptionIds,
    List<String> fileIds,
    List<Instant> times
) {
}
