package com.umc.product.feedback.application.port.in.query;

import java.util.Optional;

import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateInfo;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;

/**
 * 사용자 피드백 템플릿 조회 UseCase.
 * <p>
 * 요청자의 memberId를 기반으로 UserFeedbackTargetType(신규/기존/어드민)을 판별한 뒤,
 * 해당 context에 맞는 활성 템플릿과 Form 폼 전체 구조를 반환합니다.
 */
public interface GetUserFeedbackTemplateUseCase {

    /**
     * 요청자의 타입을 판별하여 context에 맞는 사용자 피드백 템플릿을 조회합니다.
     * <p>
     * 활성 템플릿이 없거나 현재 기수가 없는 경우 {@link Optional#empty()}를 반환합니다.
     *
     * @param requesterMemberId 요청자 Member ID (타입 판별 기준)
     * @param context           피드백 발생 컨텍스트
     * @return 해당 context + targetType의 활성 템플릿, 없으면 Optional.empty()
     */
    Optional<UserFeedbackTemplateInfo> findTemplate(Long requesterMemberId, UserFeedbackContext context);
}
