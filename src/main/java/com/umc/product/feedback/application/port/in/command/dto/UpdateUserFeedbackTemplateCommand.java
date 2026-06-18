package com.umc.product.feedback.application.port.in.command.dto;

import java.util.List;
import java.util.Objects;

import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;

import lombok.Builder;

@Builder
public record UpdateUserFeedbackTemplateCommand(
    Long templateId,
    Long requesterMemberId,
    UserFeedbackContext context,
    UserFeedbackTargetType targetType,
    String title,
    String description,
    Boolean clearDescription,
    boolean isAnonymous,
    boolean allowDuplicateResponses,
    List<FeedbackTemplateSectionEntry> sections
) {
    public UpdateUserFeedbackTemplateCommand {
        Objects.requireNonNull(templateId, "templateId must not be null");
        Objects.requireNonNull(requesterMemberId, "requesterMemberId must not be null");
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(targetType, "targetType must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(sections, "sections must not be null");
    }
}
