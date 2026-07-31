package com.umc.product.community.application.port.in.command.thread.dto;

import java.util.List;

import com.umc.product.community.domain.enums.CommunityThreadCategory;

public record CreateCommunityThreadCommand(
    Long actorMemberId,
    String title,
    String description,
    CommunityThreadCategory category,
    String icon,
    List<Long> inviteeMemberIds
) {

    public CreateCommunityThreadCommand {
        actorMemberId = CommunityThreadCommandValidation.positiveId(actorMemberId);
        title = CommunityThreadCommandValidation.requiredText(title, 80);
        description = CommunityThreadCommandValidation.optionalText(description, 500);
        category = CommunityThreadCommandValidation.required(category);
        icon = CommunityThreadCommandValidation.requiredText(icon, 32);
        inviteeMemberIds = CommunityThreadCommandValidation.uniqueIds(inviteeMemberIds, true);
        if (inviteeMemberIds.contains(actorMemberId)) {
            throw CommunityThreadCommandValidation.invalidCommand();
        }
    }
}
