package com.umc.product.form.application.port.in.command.dto;

import java.time.Instant;
import java.util.List;

import lombok.Builder;

/**
 * DRAFT FormResponse 에 개별 답변을 추가하는 Command.
 * <p>
 * 질문 타입에 따라 사용하는 필드가 다르다:
 * <ul>
 *   <li>SHORT_TEXT / LONG_TEXT: {@code textValue}</li>
 *   <li>RADIO / DROPDOWN: {@code selectedOptionIds} (1개)</li>
 *   <li>CHECKBOX: {@code selectedOptionIds} (1개 이상)</li>
 *   <li>SCHEDULE: {@code times}</li>
 *   <li>FILE: {@code fileIds}</li>
 *   <li>PORTFOLIO: {@code textValue} 또는 {@code fileIds}</li>
 * </ul>
 * 타입과 필드가 맞지 않으면 Service 단에서 검증 예외.
 */
@Builder
public record CreateAnswerCommand(
    Long formResponseId,
    Long questionId,
    String textValue,
    List<Long> selectedOptionIds,
    List<String> fileIds,
    List<Instant> times
) {
}
