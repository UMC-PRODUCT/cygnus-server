package com.umc.product.community.adapter.in.web.dto.response;

import static com.umc.product.community.adapter.in.web.CommunityWebNumbers.text;

import java.time.Instant;

import com.umc.product.community.application.port.in.query.thread.dto.ThreadSummaryInfo;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;

public record CommunityThreadSummaryResponse(
    String threadId,
    String title,
    String description,
    CommunityThreadCategory category,
    String icon,
    String memberCount,
    String unreadCount,
    String maxMembers,
    boolean isPinned,
    boolean isMuted,
    CommunityThreadMemberRole myRole,
    CommunityThreadLastMessageResponse lastMessage,
    String createdBy,
    Instant createdAt,
    Instant updatedAt
) {

    public static CommunityThreadSummaryResponse from(ThreadSummaryInfo info) {
        return new CommunityThreadSummaryResponse(
            text(info.threadId()),
            info.title(),
            info.description(),
            info.category(),
            info.icon(),
            text(info.memberCount()),
            text(info.unreadCount()),
            text(info.maxMembers()),
            info.isPinned(),
            info.isMuted(),
            info.myRole(),
            CommunityThreadLastMessageResponse.from(info.lastMessage()),
            text(info.createdBy()),
            info.createdAt(),
            info.updatedAt()
        );
    }
}
