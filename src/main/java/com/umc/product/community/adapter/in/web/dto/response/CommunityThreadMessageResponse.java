package com.umc.product.community.adapter.in.web.dto.response;

import static com.umc.product.community.adapter.in.web.CommunityWebNumbers.text;

import java.time.Instant;
import java.util.List;

import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageMentionInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageReplyInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageStatus;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageType;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadReactionInfo;

public record CommunityThreadMessageResponse(
    String messageId,
    String threadId,
    String senderId,
    String senderName,
    String content,
    CommunityThreadMessageType type,
    CommunityThreadMessageStatus status,
    List<String> fileMetadataIds,
    List<Mention> mentions,
    ReplyTo replyTo,
    List<Reaction> reactions,
    Instant createdAt,
    Instant editedAt,
    Instant deletedAt
) {

    public static CommunityThreadMessageResponse from(CommunityThreadMessageInfo info) {
        return new CommunityThreadMessageResponse(
            text(info.messageId()),
            text(info.threadId()),
            text(info.senderId()),
            info.senderName(),
            info.content(),
            info.type(),
            info.status(),
            info.fileMetadataIds(),
            info.mentions().stream().map(Mention::from).toList(),
            ReplyTo.from(info.replyTo()),
            info.reactions().stream().map(Reaction::from).toList(),
            info.createdAt(),
            info.editedAt(),
            info.deletedAt()
        );
    }

    public record Mention(String memberId, String name) {

        private static Mention from(CommunityThreadMessageMentionInfo info) {
            return new Mention(text(info.memberId()), info.name());
        }
    }

    public record ReplyTo(String messageId, String senderName, String snippet) {

        private static ReplyTo from(CommunityThreadMessageReplyInfo info) {
            return info == null ? null : new ReplyTo(
                text(info.messageId()), info.senderName(), info.snippet()
            );
        }
    }

    public record Reaction(String emoji, String count, boolean reactedByMe) {

        private static Reaction from(CommunityThreadReactionInfo info) {
            return new Reaction(info.emoji(), text(info.count()), info.reactedByMe());
        }
    }
}
