package com.umc.product.feedback.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.umc.product.feedback.application.port.out.LoadUserFeedbackTemplatePort;
import com.umc.product.feedback.domain.UserFeedbackTemplate;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.feedback.domain.exception.FeedbackDomainException;
import com.umc.product.feedback.domain.exception.FeedbackErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserFeedbackTemplatePersistenceAdapter implements LoadUserFeedbackTemplatePort {

    private final UserFeedbackTemplateJpaRepository repository;

    @Override
    public Optional<UserFeedbackTemplate> findByContextAndTargetType(
        UserFeedbackContext context,
        UserFeedbackTargetType targetType
    ) {
        return repository.findByContextAndTargetTypeAndIsActiveTrue(context, targetType);
    }

    @Override
    public Optional<UserFeedbackTemplate> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public UserFeedbackTemplate getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new FeedbackDomainException(FeedbackErrorCode.USER_FEEDBACK_TEMPLATE_NOT_FOUND));
    }
}
