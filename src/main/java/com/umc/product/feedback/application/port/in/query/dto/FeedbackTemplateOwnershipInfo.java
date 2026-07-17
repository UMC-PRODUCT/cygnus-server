package com.umc.product.feedback.application.port.in.query.dto;

import lombok.Builder;

/**
 * Form ownership policy가 확인할 Feedback template의 공개 소유권 정보.
 *
 * <p>Form 구조를 포함하지 않는 scalar projection이라 policy가 Form query를 재귀 호출하지
 * 않는다. Feedback 외부 계층에는 template ID와 연결된 Form ID, 활성 상태만 노출한다.</p>
 */
@Builder
public record FeedbackTemplateOwnershipInfo(
    Long templateId,
    Long formId,
    boolean active
) {
}
