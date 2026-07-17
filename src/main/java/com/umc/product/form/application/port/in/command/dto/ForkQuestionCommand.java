package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 기존 질문을 기반으로 새 버전을 생성하는 Command (Copy-on-Write).
 * 원본 질문의 모든 속성과 선택지를 복사하며, 원본은 비활성화된다.
 */
@Builder
public record ForkQuestionCommand(
    Long originQuestionId
) {
}
