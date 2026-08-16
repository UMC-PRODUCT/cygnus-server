package com.umc.product.community.application.port.in.command.thread.dto;

import java.time.Instant;

import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;

public record CommunityThreadLifecycleInfo(
    Long threadId,
    String title,
    String description,
    CommunityThreadCategory category,
    String icon,
    Long createdBy,
    long memberCount,
    int maxMembers,
    Long actorMemberId,
    CommunityThreadMemberRole actorRole,
    boolean pinned,
    boolean muted,
    Instant deletedAt
) {

    public static CommunityThreadLifecycleInfo from(
        CommunityThread thread,
        CommunityThreadMember actor,
        long memberCount,
        int maxMembers
    ) {
        return new CommunityThreadLifecycleInfo(
            thread.getId(),
            thread.getTitle(),
            thread.getDescription(),
            thread.getCategory(),
            thread.getIcon(),
            thread.getCreatorMemberId(),
            memberCount,
            maxMembers,
            actor.getMemberId(),
            actor.getRole(),
            actor.isPinned(),
            actor.isMuted(),
            thread.getDeletedAt()
        );
    }
}
