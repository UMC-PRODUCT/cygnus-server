package com.umc.product.form.application.port.in.command.dto;

import lombok.Builder;

/**
 * 폼 메타데이터 부분 업데이트 Command (임시저장 / 발행 후 모두 사용).
 * <p>
 * null 인 필드는 '변경 없음' 으로 처리 (PATCH 의미).
 * 임시저장 상태에서는 어느 필드든 부분 변경이 가능해야 하므로 모든 필드를 nullable 로 둔다.
 * {@code allowDuplicateResponses}가 null이면 기존 중복 응답 정책을 유지한다.
 * <p>
 * {@code requesterMemberId} 는 리뷰 협의를 통해 권한 검증을 form 측에서 하지 않는 것이 확정되면 삭제 예정 (권한 검증은 호출 측 책임).
 */
@Builder
public record UpdateFormCommand(
    Long formId,
    Long requesterMemberId,
    String title,
    String description,
    Boolean clearDescription,
    Boolean isAnonymous,
    Boolean allowDuplicateResponses
) {
}
