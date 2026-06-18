package com.umc.product.feedback.adapter.in.web.dto.request;

import java.util.List;

import com.umc.product.feedback.adapter.in.web.dto.common.FeedbackTemplateSectionItem;
import com.umc.product.feedback.application.port.in.command.dto.UpdateUserFeedbackTemplateCommand;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserFeedbackTemplateRequest(
    @NotNull(message = "context는 필수입니다")
    UserFeedbackContext context,

    @NotNull(message = "targetType은 필수입니다")
    UserFeedbackTargetType targetType,

    @NotBlank(message = "폼 제목은 필수입니다")
    @Size(max = 200, message = "폼 제목은 200자 이하여야 합니다")
    String title,

    @Size(max = 500, message = "폼 설명은 500자 이하여야 합니다")
    String description,

    boolean isAnonymous,

    boolean allowDuplicateResponses,

    @NotNull(message = "sections 필드는 필수입니다")
    @Valid
    List<FeedbackTemplateSectionItem> sections
) {
    public UpdateUserFeedbackTemplateCommand toCommand(Long templateId, Long requesterMemberId) {
        return UpdateUserFeedbackTemplateCommand.builder()
            .templateId(templateId)
            .requesterMemberId(requesterMemberId)
            .context(context)
            .targetType(targetType)
            .title(title)
            .description(description)
            .clearDescription(description == null)
            .isAnonymous(isAnonymous)
            .allowDuplicateResponses(allowDuplicateResponses)
            .sections(sections.stream().map(FeedbackTemplateSectionItem::toEntry).toList())
            .build();
    }
}
