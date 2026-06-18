package com.umc.product.feedback.application.service.query;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.feedback.application.port.in.query.GetUserFeedbackTemplateUseCase;
import com.umc.product.feedback.application.port.in.query.dto.UserFeedbackTemplateInfo;
import com.umc.product.feedback.application.port.out.LoadUserFeedbackTemplatePort;
import com.umc.product.feedback.application.service.UserFeedbackAudienceResolver;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.form.application.port.in.query.GetFormUseCase;

import lombok.RequiredArgsConstructor;

/**
 * 사용자 피드백 템플릿 조회 서비스.
 * <p>
 * 요청자의 audience를 판별하고,
 * 해당 context + targetType 조합의 활성 UserFeedbackTemplate을 조회하여 Form 폼 구조와 함께 반환합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserFeedbackTemplateQueryService implements GetUserFeedbackTemplateUseCase {

    private final LoadUserFeedbackTemplatePort loadUserFeedbackTemplatePort;

    // Cross-domain
    private final UserFeedbackAudienceResolver audienceResolver;
    private final GetFormUseCase getFormUseCase;

    @Override
    public Optional<UserFeedbackTemplateInfo> findTemplate(Long requesterMemberId, UserFeedbackContext context) {
        return audienceResolver.resolve(requesterMemberId)
            .flatMap(targetType -> loadUserFeedbackTemplatePort.findByContextAndTargetType(context, targetType)
                .map(template -> UserFeedbackTemplateInfo.builder()
                    .templateId(template.getId())
                    .context(template.getContext())
                    .targetType(template.getTargetType())
                    .form(getFormUseCase.getFormWithStructure(template.getFormId()))
                    .build()));
    }
}
