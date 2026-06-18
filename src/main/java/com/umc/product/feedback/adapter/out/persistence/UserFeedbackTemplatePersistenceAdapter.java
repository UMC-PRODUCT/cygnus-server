package com.umc.product.feedback.adapter.out.persistence;

import java.util.Optional;
import java.util.List;

import org.springframework.stereotype.Component;

import com.umc.product.feedback.application.port.out.LoadUserFeedbackTemplatePort;
import com.umc.product.feedback.application.port.out.SaveUserFeedbackTemplatePort;
import com.umc.product.feedback.domain.UserFeedbackTemplate;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;
import com.umc.product.feedback.domain.exception.FeedbackDomainException;
import com.umc.product.feedback.domain.exception.FeedbackErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserFeedbackTemplatePersistenceAdapter implements LoadUserFeedbackTemplatePort, SaveUserFeedbackTemplatePort {

    private final UserFeedbackTemplateJpaRepository repository;
    private final UserFeedbackTemplateQueryRepository queryRepository;

    @Override
    public Optional<UserFeedbackTemplate> findByContextAndTargetType(UserFeedbackContext context, UserFeedbackTargetType targetType) {
        return repository.findByContextAndTargetTypeAndIsActiveTrue(context, targetType);
    }

    @Override
    public UserFeedbackTemplate getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new FeedbackDomainException(FeedbackErrorCode.USER_FEEDBACK_TEMPLATE_NOT_FOUND));
    }

    @Override
    public UserFeedbackTemplate getActiveById(Long id) {
        UserFeedbackTemplate template = getById(id);
        if (!template.isActive()) {
            throw new FeedbackDomainException(FeedbackErrorCode.USER_FEEDBACK_TEMPLATE_INACTIVE);
        }
        return template;
    }

    @Override
    public List<UserFeedbackTemplate> listByCondition(
        UserFeedbackContext context,
        UserFeedbackTargetType targetType,
        Boolean active
    ) {
        return queryRepository.findByCondition(context, targetType, active);
    }

    @Override
    public boolean existsActiveByContextAndTargetType(
        UserFeedbackContext context,
        UserFeedbackTargetType targetType
    ) {
        return repository.existsByContextAndTargetTypeAndIsActiveTrue(context, targetType);
    }

    @Override
    public boolean existsActiveByContextAndTargetTypeExcludingId(
        UserFeedbackContext context,
        UserFeedbackTargetType targetType,
        Long excludedTemplateId
    ) {
        return repository.existsByContextAndTargetTypeAndIsActiveTrueAndIdNot(
            context,
            targetType,
            excludedTemplateId
        );
    }

    @Override
    public UserFeedbackTemplate save(UserFeedbackTemplate template) {
        return repository.save(template);
    }
}
