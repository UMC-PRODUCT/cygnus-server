package com.umc.product.form.application.port.in.command.dto;

import java.time.Instant;
import java.util.List;

import lombok.Builder;

/**
 * 한 질문에 대한 응답 입력값.
 *
 * <ul>
 *   <li>SHORT_TEXT / LONG_TEXT: {@code textValue} 사용</li>
 *   <li>RADIO / DROPDOWN: {@code selectedOptionIds} 에 1개</li>
 *   <li>CHECKBOX: {@code selectedOptionIds} 에 1개 이상</li>
 *   <li>SCHEDULE: {@code times} 에 15분 배수 Instant 목록</li>
 *   <li>FILE: {@code fileIds} 에 1개 이상</li>
 *   <li>PORTFOLIO: {@code textValue} (링크) 또는 {@code fileIds} — 둘 중 하나</li>
 * </ul>
 */
@Builder
public record AnswerCommand(
    Long questionId,
    String textValue,
    List<Long> selectedOptionIds,
    List<String> fileIds,
    List<Instant> times
) {
}
