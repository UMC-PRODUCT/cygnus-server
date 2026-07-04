package com.umc.product.feedback.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.feedback.application.port.in.command.SubmitUserFeedbackResponseUseCase;
import com.umc.product.feedback.application.port.in.command.dto.SubmitUserFeedbackResponseCommand;
import com.umc.product.feedback.application.port.out.LoadUserFeedbackTemplatePort;
import com.umc.product.feedback.domain.UserFeedbackTemplate;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.form.application.port.in.command.ManageFormResponseUseCase;
import com.umc.product.form.application.port.in.command.dto.SubmitFormResponseCommand;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserFeedbackResponseCommandService implements SubmitUserFeedbackResponseUseCase {

    private final LoadUserFeedbackTemplatePort loadUserFeedbackTemplatePort;
    private final ManageFormResponseUseCase manageFormResponseUseCase;

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

        return manageFormResponseUseCase.submitImmediately(
            SubmitFormResponseCommand.builder()
                .formId(template.getFormId())
                .respondentMemberId(command.respondentMemberId())
                .answers(command.answers())
                .build()
        );
    }
}
