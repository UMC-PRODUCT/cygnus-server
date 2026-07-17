package com.umc.product.feedback.application.port.in.query;

import java.util.Optional;

import com.umc.product.feedback.application.port.in.query.dto.FeedbackTemplateOwnershipInfo;

/**
 * Feedback template의 Form ownership 정책이 사용하는 public scalar query.
 *
 * <p>Form core가 Feedback persistence adapter나 entity를 직접 참조하지 않도록 consumer가
 * 필요한 소유권 projection만 제공한다.</p>
 */
public interface GetUserFeedbackTemplateOwnershipUseCase {

    Optional<FeedbackTemplateOwnershipInfo> findOwnershipById(Long templateId);
}
