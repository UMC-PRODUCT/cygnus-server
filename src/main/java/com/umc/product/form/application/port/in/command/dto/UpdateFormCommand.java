package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 폼 메타데이터 부분 업데이트 Command (임시저장 / 발행 후 모두 사용).
 * <p>
 * null 인 필드는 '변경 없음' 으로 처리 (PATCH 의미).
 * 임시저장 상태에서는 어느 필드든 부분 변경이 가능해야 하므로 모든 필드를 nullable 로 둔다.
 * {@code allowDuplicateResponses}가 null이면 기존 중복 응답 정책을 유지한다.
 * 권한 주체와 expected owner는 command가 아니라 application port의 별도 인자로 전달된다.
 */
@Builder
public record UpdateFormCommand(
    Long formId,
    String title,
    String description,
    Boolean clearDescription,
    Boolean isAnonymous,
    Boolean allowDuplicateResponses
) {
}
