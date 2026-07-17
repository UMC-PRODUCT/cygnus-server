package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 폼에 대한 draft 응답을 최초 생성하는 Command.
 * <p>
 * 빈 draft 를 생성하고 ID를 반환한다. 이후 {@code updateDraft} 로 답변을 추가하며 {@code submitDraft} 로 최종 제출한다.
 * <p>
 * 폼이 중복 응답을 허용하지 않으면 actor context의 authenticated member 응답이 이미 있을 때 예외.
 */
@Builder
public record CreateDraftFormResponseCommand(
    Long formId
) {
}
