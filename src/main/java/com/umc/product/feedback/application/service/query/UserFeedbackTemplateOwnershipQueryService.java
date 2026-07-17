package com.umc.product.feedback.application.service.query;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.feedback.application.port.in.query.GetUserFeedbackTemplateOwnershipUseCase;
import com.umc.product.feedback.application.port.in.query.dto.FeedbackTemplateOwnershipInfo;
import com.umc.product.feedback.application.port.out.LoadUserFeedbackTemplatePort;

import lombok.RequiredArgsConstructor;

/**
 * Form ownership policy용 Feedback template scalar 조회 facade.
 *
 * <p>Form 구조 조회 UseCase와 분리해 policy registry가 Form query를 다시 참조하는 순환 의존을
 * 만들지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserFeedbackTemplateOwnershipQueryService implements GetUserFeedbackTemplateOwnershipUseCase {

    private final LoadUserFeedbackTemplatePort loadUserFeedbackTemplatePort;

    @Override
    public Optional<FeedbackTemplateOwnershipInfo> findOwnershipById(Long templateId) {
        if (templateId == null || templateId <= 0) {
            return Optional.empty();
        }
        return loadUserFeedbackTemplatePort.findById(templateId)
            .map(template -> FeedbackTemplateOwnershipInfo.builder()
                .templateId(template.getId())
                .formId(template.getFormId())
                .active(template.isActive())
                .build());
    }
}
