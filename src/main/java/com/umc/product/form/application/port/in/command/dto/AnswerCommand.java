package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

import java.util.List;

/**
 * 한 질문에 대한 응답 입력값.
 *
 * <ul>
 *   <li>SHORT_TEXT / LONG_TEXT: {@code textValue} 사용</li>
 *   <li>RADIO / DROPDOWN: {@code selectedOptionIds} 에 1개</li>
 *   <li>CHECKBOX: {@code selectedOptionIds} 에 1개 이상</li>
 *   <li>FILE: {@code fileIds} 에 1개 이상</li>
 *   <li>PORTFOLIO: {@code textValue} (링크) 또는 {@code fileIds} — 둘 중 하나</li>
 * </ul>
 * <p>
 * SCHEDULE 타입은 현재 미지원 (후속 PR).
 */
@Builder
public record AnswerCommand(
    Long questionId,
    String textValue,
    List<Long> selectedOptionIds,
    List<String> fileIds
) {
}
