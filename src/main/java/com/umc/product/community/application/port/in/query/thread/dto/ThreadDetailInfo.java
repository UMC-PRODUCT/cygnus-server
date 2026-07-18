package com.umc.product.community.application.port.in.query.thread.dto;

import java.time.Instant;

import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;

public record ThreadDetailInfo(
    Long threadId,
    String title,
    String description,
    CommunityThreadCategory category,
    String icon,
    long memberCount,
    long unreadCount,
    int maxMembers,
    boolean isPinned,
    boolean isMuted,
    CommunityThreadMemberRole myRole,
    ThreadLastMessageInfo lastMessage,
    Long createdBy,
    Instant createdAt,
    Instant updatedAt,
    String shareUrl,
    Instant deletedAt
) {

    public static ThreadDetailInfo from(ThreadSummaryInfo summary, String shareUrl, Instant deletedAt) {
        return new ThreadDetailInfo(
            summary.threadId(),
            summary.title(),
            summary.description(),
            summary.category(),
            summary.icon(),
            summary.memberCount(),
            summary.unreadCount(),
            summary.maxMembers(),
            summary.isPinned(),
            summary.isMuted(),
            summary.myRole(),
            summary.lastMessage(),
            summary.createdBy(),
            summary.createdAt(),
            summary.updatedAt(),
            shareUrl,
            deletedAt
        );
    }
}
