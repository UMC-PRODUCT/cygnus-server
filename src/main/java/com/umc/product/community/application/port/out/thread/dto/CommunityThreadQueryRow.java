package com.umc.product.community.application.port.out.thread.dto;

import java.time.Instant;

import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.enums.CommunityThreadMemberState;

public record CommunityThreadQueryRow(
    Long threadId,
    String title,
    String description,
    CommunityThreadCategory category,
    String icon,
    long memberCount,
    long unreadCount,
    boolean pinned,
    boolean muted,
    CommunityThreadMemberRole requesterRole,
    CommunityThreadMemberState requesterState,
    String lastMessagePreview,
    Long lastMessageSenderMemberId,
    Instant lastMessageCreatedAt,
    Long creatorMemberId,
    Instant lastActivityAt,
    Instant deletedAt,
    Instant createdAt,
    Instant updatedAt
) {
}
