package com.umc.product.feedback.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.feedback.application.authorization.FeedbackPolicyAuthorizationService;
import com.umc.product.feedback.application.port.in.command.SubmitUserFeedbackResponseUseCase;
import com.umc.product.feedback.application.port.in.command.dto.SubmitUserFeedbackResponseCommand;
import com.umc.product.feedback.application.port.out.LoadUserFeedbackTemplatePort;
import com.umc.product.feedback.domain.UserFeedbackTemplate;
import com.umc.product.feedback.domain.exception.FeedbackDomainException;
import com.umc.product.feedback.domain.exception.FeedbackErrorCode;
import com.umc.product.form.application.port.in.command.ManageFormResponseUseCase;
import com.umc.product.form.application.port.in.command.dto.SubmitFormResponseCommand;
import com.umc.product.global.exception.constant.Domain;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserFeedbackResponseCommandService implements SubmitUserFeedbackResponseUseCase {

    private final LoadUserFeedbackTemplatePort loadUserFeedbackTemplatePort;
    private final ManageFormResponseUseCase manageFormResponseUseCase;
    private final FeedbackPolicyAuthorizationService policyAuthorizationService;

    @Audited(
        domain = Domain.FEEDBACK,
        action = AuditAction.SUBMIT,
        targetType = "UserFeedbackResponse",
        targetId = "#result",
        description = "'사용자 피드백 응답을 제출했습니다.'"
    )
    @Override
    public Long submit(SubmitUserFeedbackResponseCommand command) {
        UserFeedbackTemplate template = loadUserFeedbackTemplatePort.getById(command.templateId());
        if (!policyAuthorizationService.canSubmit(
            command.respondentMemberId(),
            template.getTargetType()
        )) {
            throw new FeedbackDomainException(
                FeedbackErrorCode.USER_FEEDBACK_RESPONSE_FORBIDDEN);
        }

        return manageFormResponseUseCase.submitImmediately(
            SubmitFormResponseCommand.builder()
                .formId(template.getFormId())
                .respondentMemberId(command.respondentMemberId())
                .answers(command.answers())
                .build()
        );
    }
}
