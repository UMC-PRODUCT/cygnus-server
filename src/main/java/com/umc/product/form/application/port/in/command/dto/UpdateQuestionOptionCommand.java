package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 선택지 속성 업데이트 Command. content / isOther / nextSectionId를 부분 수정.
 * null 인 필드는 기존 값 유지. nextSectionId를 제거하려면 {@code clearNextSectionId=true}.
 * 순서 변경은 {@code ReorderQuestionOptionsCommand}.
 */
@Builder
public record UpdateQuestionOptionCommand(
    Long optionId,
    Long requesterMemberId,
    String content,
    Boolean isOther,
    Long nextSectionId,
    Boolean clearNextSectionId
) {
}
