package com.umc.product.community.domain;

import java.time.Instant;

import com.umc.product.common.BaseEntity;
import com.umc.product.community.domain.enums.CommunityThreadCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "community_thread",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_community_thread_chat_room",
        columnNames = "chat_room_id"
    ),
    indexes = @Index(
        name = "idx_community_thread_active_list",
        columnList = "deleted_at, last_activity_at, id"
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityThread extends BaseEntity {

    private static final int MAX_TITLE_CODE_POINTS = 80;
    private static final int MAX_DESCRIPTION_CODE_POINTS = 500;
    private static final int MAX_ICON_CODE_POINTS = 32;
    private static final int MAX_PREVIEW_CODE_POINTS = 2_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityThreadCategory category;

    @Column(nullable = false, length = 128)
    private String icon;

    @Column(name = "creator_member_id", nullable = false)
    private Long creatorMemberId;

    @Column(name = "last_message_id")
    private Long lastMessageId;

    @Column(name = "last_message_preview", length = 2_000)
    private String lastMessagePreview;

    @Column(name = "last_message_sender_member_id")
    private Long lastMessageSenderMemberId;

    @Column(name = "last_message_created_at")
    private Instant lastMessageCreatedAt;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private CommunityThread(
        Long chatRoomId,
        String title,
        String description,
        CommunityThreadCategory category,
        String icon,
        Long creatorMemberId,
        Instant createdAt
    ) {
        this.chatRoomId = requirePositive(chatRoomId, "chatRoomId");
        this.title = normalizeRequired(title, MAX_TITLE_CODE_POINTS, "title");
        this.description = normalizeOptional(description, MAX_DESCRIPTION_CODE_POINTS, "description");
        this.category = requireCategory(category);
        this.icon = normalizeRequired(icon, MAX_ICON_CODE_POINTS, "icon");
        this.creatorMemberId = requirePositive(creatorMemberId, "creatorMemberId");
        this.lastActivityAt = requireInstant(createdAt, "createdAt");
    }

    public static CommunityThread create(
        Long chatRoomId,
        String title,
        String description,
        CommunityThreadCategory category,
        String icon,
        Long creatorMemberId,
        Instant createdAt
    ) {
        return new CommunityThread(
            chatRoomId,
            title,
            description,
            category,
            icon,
            creatorMemberId,
            createdAt
        );
    }

    public void updateMetadata(
        String title,
        String description,
        CommunityThreadCategory category,
        String icon
    ) {
        this.title = normalizeRequired(title, MAX_TITLE_CODE_POINTS, "title");
        this.description = normalizeOptional(description, MAX_DESCRIPTION_CODE_POINTS, "description");
        this.category = requireCategory(category);
        this.icon = normalizeRequired(icon, MAX_ICON_CODE_POINTS, "icon");
    }

    public void updateLastMessage(
        Long messageId,
        String preview,
        Long senderMemberId,
        Instant messageCreatedAt
    ) {
        Long validMessageId = requirePositive(messageId, "messageId");
        String validPreview = normalizePreview(preview);
        Long validSenderMemberId = requirePositive(senderMemberId, "senderMemberId");
        Instant validMessageCreatedAt = requireInstant(messageCreatedAt, "messageCreatedAt");
        if (lastMessageId != null && validMessageId < lastMessageId) {
            return;
        }
        this.lastMessageId = validMessageId;
        this.lastMessagePreview = validPreview;
        this.lastMessageSenderMemberId = validSenderMemberId;
        this.lastMessageCreatedAt = validMessageCreatedAt;
        if (lastActivityAt.isBefore(validMessageCreatedAt)) {
            this.lastActivityAt = validMessageCreatedAt;
        }
    }

    public void touchActivity(Instant activityAt) {
        Instant validActivityAt = requireInstant(activityAt, "activityAt");
        if (lastActivityAt.isBefore(validActivityAt)) {
            lastActivityAt = validActivityAt;
        }
    }

    public void delete(Instant deletedAt) {
        if (this.deletedAt == null) {
            this.deletedAt = requireInstant(deletedAt, "deletedAt");
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    private static CommunityThreadCategory requireCategory(CommunityThreadCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("category must not be null");
        }
        return category;
    }

    private static Long requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static Instant requireInstant(Instant value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }

    private static String normalizeRequired(String value, int maxCodePoints, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        String normalized = value.strip();
        validateCodePointLength(normalized, maxCodePoints, name);
        return normalized;
    }

    private static String normalizeOptional(String value, int maxCodePoints, String name) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        validateCodePointLength(normalized, maxCodePoints, name);
        return normalized;
    }

    private static String normalizePreview(String preview) {
        String normalized = preview == null ? "" : preview;
        validateCodePointLength(normalized, MAX_PREVIEW_CODE_POINTS, "preview");
        return normalized;
    }

    private static void validateCodePointLength(String value, int maxCodePoints, String name) {
        if (value.codePointCount(0, value.length()) > maxCodePoints) {
            throw new IllegalArgumentException(name + " is too long");
        }
    }
}
