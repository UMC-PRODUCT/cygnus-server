package com.umc.product.feedback.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.feedback.domain.UserFeedbackTemplate;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;

public interface UserFeedbackTemplateJpaRepository extends JpaRepository<UserFeedbackTemplate, Long> {

    Optional<UserFeedbackTemplate> findByContextAndTargetTypeAndIsActiveTrue(UserFeedbackContext context, UserFeedbackTargetType targetType);

    boolean existsByContextAndTargetTypeAndIsActiveTrue(UserFeedbackContext context, UserFeedbackTargetType targetType);

    boolean existsByContextAndTargetTypeAndIsActiveTrueAndIdNot(
        UserFeedbackContext context,
        UserFeedbackTargetType targetType,
        Long id
    );
}
