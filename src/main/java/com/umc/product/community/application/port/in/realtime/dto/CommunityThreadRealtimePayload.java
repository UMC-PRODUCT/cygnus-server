package com.umc.product.community.application.port.in.realtime.dto;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.umc.product.community.application.port.in.query.thread.dto.ThreadSummaryInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadReactionInfo;
import com.umc.product.community.domain.enums.CommunityThreadCategory;

public sealed interface CommunityThreadRealtimePayload {

    record CommandAcknowledged(
        UUID commandId,
        CommunityThreadRealtimeCommandType command,
        Long messageId,
        UUID clientMessageId,
        boolean deduplicated
    ) implements CommunityThreadRealtimePayload {

        public CommandAcknowledged {
            Objects.requireNonNull(commandId, "commandId must not be null");
            Objects.requireNonNull(command, "command must not be null");
        }
    }

    record MessageCreated(
        CommunityThreadMessageInfo message,
        UUID clientMessageId
    ) implements CommunityThreadRealtimePayload {

        public MessageCreated {
            Objects.requireNonNull(message, "message must not be null");
        }
    }

    record MessageUpdated(
        CommunityThreadMessageInfo message
    ) implements CommunityThreadRealtimePayload {

        public MessageUpdated {
            Objects.requireNonNull(message, "message must not be null");
        }
    }

    record MessageDeleted(
        CommunityThreadMessageInfo message
    ) implements CommunityThreadRealtimePayload {

        public MessageDeleted {
            Objects.requireNonNull(message, "message must not be null");
        }
    }

    record ReactionChanged(
        Long messageId,
        List<CommunityThreadReactionInfo> reactions
    ) implements CommunityThreadRealtimePayload {

        public ReactionChanged {
            messageId = requirePositive(messageId, "messageId");
            reactions = List.copyOf(reactions);
        }
    }

    record ReadUpdated(
        Long memberId,
        Long lastReadMessageId
    ) implements CommunityThreadRealtimePayload {

        public ReadUpdated {
            memberId = requirePositive(memberId, "memberId");
            lastReadMessageId = requirePositive(lastReadMessageId, "lastReadMessageId");
        }
    }

    record ThreadInvited(
        ThreadSummaryInfo thread
    ) implements CommunityThreadRealtimePayload {

        public ThreadInvited {
            Objects.requireNonNull(thread, "thread must not be null");
        }
    }

    record ThreadUpdated(
        String threadId,
        String title,
        String description,
        CommunityThreadCategory category,
        String icon,
        long memberCount,
        int maxMembers,
        Instant lastActivityAt,
        Instant updatedAt
    ) implements CommunityThreadRealtimePayload {

        public ThreadUpdated {
            Objects.requireNonNull(threadId, "threadId must not be null");
            Objects.requireNonNull(category, "category must not be null");
            Objects.requireNonNull(lastActivityAt, "lastActivityAt must not be null");
            Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        }
    }

    record ThreadDeleted(
        String threadId,
        Instant deletedAt
    ) implements CommunityThreadRealtimePayload {

        public ThreadDeleted {
            Objects.requireNonNull(threadId, "threadId must not be null");
            Objects.requireNonNull(deletedAt, "deletedAt must not be null");
        }
    }

    record MemberKicked(
        Long memberId,
        long memberCount
    ) implements CommunityThreadRealtimePayload {

        public MemberKicked {
            memberId = requirePositive(memberId, "memberId");
        }
    }

    record MemberLeft(
        Long memberId,
        long memberCount
    ) implements CommunityThreadRealtimePayload {

        public MemberLeft {
            memberId = requirePositive(memberId, "memberId");
        }
    }

    private static Long requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }
}
