package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * DRAFT 상태의 폼을 최초 생성하는 Command.
 * {@code title}은 필수 (최초 생성 후 업데이트 가능). {@code description}은 선택.
 *      TODO: form.title가 nullable = false임. 최초 생성 시에 아무 값도 넘겨주고 싶지 않다면 nullable로 변경.
 * {@code isAnonymous}는 응답자 공개 여부 (true=익명).
 * {@code allowDuplicateResponses}는 같은 폼에 같은 응답자가 여러 응답을 만들 수 있는지 여부.
 */
@Builder
public record CreateDraftFormCommand(
    Long createdMemberId,
    String title,
    String description,
    boolean isAnonymous,
    boolean allowDuplicateResponses
) {
}
