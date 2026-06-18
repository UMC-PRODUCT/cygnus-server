package com.umc.product.feedback.application.port.out;

import java.util.Optional;
import java.util.List;

import com.umc.product.feedback.domain.UserFeedbackTemplate;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;

/**
 * UserFeedbackTemplate 조회 Port (Driven / Port Out).
 * <p>
 * 메서드 prefix 규칙:
 * <ul>
 *   <li>{@code findBy*} — 없어도 정상 ({@link Optional})</li>
 *   <li>{@code getBy*} — 반드시 있어야 하며 없으면 {@code FeedbackDomainException} (USER_FEEDBACK_TEMPLATE_NOT_FOUND)</li>
 * </ul>
 */
public interface LoadUserFeedbackTemplatePort {

    /**
     * context + targetType 조합으로 활성 템플릿을 단건 조회합니다. 없으면 Optional.empty().
     */
    Optional<UserFeedbackTemplate> findByContextAndTargetType(UserFeedbackContext context, UserFeedbackTargetType targetType);

    /**
     * ID로 UserFeedbackTemplate을 조회합니다. 존재하지 않으면 도메인 예외를 던집니다.
     */
    UserFeedbackTemplate getById(Long id);

    /**
     * ID로 활성 UserFeedbackTemplate을 조회합니다. 존재하지 않거나 비활성이면 도메인 예외를 던집니다.
     */
    UserFeedbackTemplate getActiveById(Long id);

    /**
     * 관리자 화면에서 optional 필터로 템플릿 목록을 조회합니다.
     */
    List<UserFeedbackTemplate> listByCondition(
        UserFeedbackContext context,
        UserFeedbackTargetType targetType,
        Boolean active
    );

    boolean existsActiveByContextAndTargetType(UserFeedbackContext context, UserFeedbackTargetType targetType);

    boolean existsActiveByContextAndTargetTypeExcludingId(
        UserFeedbackContext context,
        UserFeedbackTargetType targetType,
        Long excludedTemplateId
    );
}
