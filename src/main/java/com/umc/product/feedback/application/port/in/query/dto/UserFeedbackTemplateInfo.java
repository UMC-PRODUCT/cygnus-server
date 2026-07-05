package com.umc.product.feedback.application.port.in.query.dto;

import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.form.application.port.in.query.dto.FormWithStructureInfo;

import lombok.Builder;

/**
 * 사용자 피드백 템플릿 조회 결과 DTO.
 * <p>
 * Form 폼의 전체 구조(섹션->질문->옵션)를 포함하며, 응답 제출 시 필요한 templateId도 함께 제공합니다.
 */
@Builder
public record UserFeedbackTemplateInfo(
    Long templateId,
    UserFeedbackContext context,
    UserFeedbackTargetType targetType,
    FormWithStructureInfo form
) {
}
