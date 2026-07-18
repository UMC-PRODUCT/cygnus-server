package com.umc.product.community.application.port.in.command.thread.dto;

import com.umc.product.community.domain.enums.CommunityThreadCategory;

public record UpdateCommunityThreadCommand(
    Long threadId,
    Long actorMemberId,
    String title,
    String description,
    boolean descriptionProvided,
    CommunityThreadCategory category,
    String icon
) {

    public UpdateCommunityThreadCommand(
        Long threadId,
        Long actorMemberId,
        String title,
        String description,
        CommunityThreadCategory category,
        String icon
    ) {
        this(threadId, actorMemberId, title, description, description != null, category, icon);
    }

    public UpdateCommunityThreadCommand {
        threadId = CommunityThreadCommandValidation.positiveId(threadId);
        actorMemberId = CommunityThreadCommandValidation.positiveId(actorMemberId);
        if (title == null && !descriptionProvided && category == null && icon == null) {
            throw CommunityThreadCommandValidation.invalidCommand();
        }
        title = title == null ? null : CommunityThreadCommandValidation.requiredText(title, 80);
        description = normalizeDescription(description);
        icon = icon == null ? null : CommunityThreadCommandValidation.requiredText(icon, 32);
    }

    private static String normalizeDescription(String value) {
        String normalized = CommunityThreadCommandValidation.optionalText(value, 500);
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }
}
