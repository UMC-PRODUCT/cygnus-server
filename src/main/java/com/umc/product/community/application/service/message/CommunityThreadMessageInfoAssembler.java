package com.umc.product.community.application.service.message;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageReplyInfo;
import com.umc.product.chat.application.port.in.query.dto.ChatReactionInfo;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageMentionInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageReplyInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageStatus;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageType;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadReactionInfo;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

import lombok.RequiredArgsConstructor;

/**
 * Chat 엔진의 조회 모델을 Community thread 공개 모델로 변환한다.
 *
 * <p>Chat room id는 Community 외부 계약에 포함하지 않고, 이름이 필요한 멤버를 한 번의
 * batch query로 조립한다.</p>
 */
@Component
@RequiredArgsConstructor
public class CommunityThreadMessageInfoAssembler {

    private static final String UNKNOWN_MEMBER_NAME = "알 수 없음";

    private final GetMemberUseCase getMemberUseCase;

    public CommunityThreadMessageInfo assemble(Long threadId, ChatMessageInfo message) {
        return assemble(threadId, List.of(message)).get(0);
    }

    public List<CommunityThreadMessageInfo> assemble(Long threadId, List<ChatMessageInfo> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        Map<Long, MemberInfo> members = loadMembers(messages);
        return messages.stream()
            .map(message -> toInfo(threadId, message, members))
            .toList();
    }

    public Map<Long, CommunityThreadMessageInfo> assembleForRecipients(
        Long threadId,
        Map<Long, ChatMessageInfo> messagesByRecipient
    ) {
        if (messagesByRecipient == null || messagesByRecipient.isEmpty()) {
            return Map.of();
        }

        Map<Long, MemberInfo> members = loadMembers(List.copyOf(messagesByRecipient.values()));
        Map<Long, CommunityThreadMessageInfo> result = new LinkedHashMap<>();
        messagesByRecipient.forEach((recipientMemberId, message) ->
            result.put(recipientMemberId, toInfo(threadId, message, members)));
        return Collections.unmodifiableMap(result);
    }

    private Map<Long, MemberInfo> loadMembers(List<ChatMessageInfo> messages) {
        Set<Long> memberIds = new LinkedHashSet<>();
        messages.forEach(message -> {
            if (message.senderMemberId() != null) {
                memberIds.add(message.senderMemberId());
            }
            if (message.mentionedMemberIds() != null) {
                memberIds.addAll(message.mentionedMemberIds());
            }
            ChatMessageReplyInfo reply = message.replyTo();
            if (reply != null && reply.senderMemberId() != null) {
                memberIds.add(reply.senderMemberId());
            }
        });
        return memberIds.isEmpty() ? Map.of() : getMemberUseCase.findAllByIds(memberIds);
    }

    private CommunityThreadMessageInfo toInfo(
        Long threadId,
        ChatMessageInfo message,
        Map<Long, MemberInfo> members
    ) {
        List<CommunityThreadMessageMentionInfo> mentions = message.mentionedMemberIds().stream()
            .map(memberId -> new CommunityThreadMessageMentionInfo(memberId, memberName(members, memberId)))
            .toList();

        CommunityThreadMessageReplyInfo replyTo = toReplyInfo(message.replyTo(), members);
        List<CommunityThreadReactionInfo> reactions = message.reactions().stream()
            .map(this::toReactionInfo)
            .toList();

        return new CommunityThreadMessageInfo(
            message.messageId(),
            threadId,
            message.senderMemberId(),
            memberName(members, message.senderMemberId()),
            message.content(),
            toCommunityType(message.contentType()),
            CommunityThreadMessageStatus.SENT,
            message.fileMetadataIds(),
            mentions,
            replyTo,
            reactions,
            message.clientMessageId(),
            message.createdAt(),
            message.editedAt(),
            message.deletedAt()
        );
    }

    private CommunityThreadMessageReplyInfo toReplyInfo(
        ChatMessageReplyInfo reply,
        Map<Long, MemberInfo> members
    ) {
        if (reply == null) {
            return null;
        }
        return new CommunityThreadMessageReplyInfo(
            reply.messageId(),
            memberName(members, reply.senderMemberId()),
            reply.snippet()
        );
    }

    private CommunityThreadReactionInfo toReactionInfo(ChatReactionInfo reaction) {
        return new CommunityThreadReactionInfo(
            reaction.emoji(),
            reaction.count(),
            reaction.reactedByMe()
        );
    }

    private CommunityThreadMessageType toCommunityType(MessageContentType contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("contentType must not be null");
        }
        return switch (contentType) {
            case TEXT -> CommunityThreadMessageType.TEXT;
            case IMAGE, FILE -> CommunityThreadMessageType.IMAGE;
            case SYSTEM -> CommunityThreadMessageType.SYSTEM;
        };
    }

    private String memberName(Map<Long, MemberInfo> members, Long memberId) {
        MemberInfo member = members.get(memberId);
        return member == null || member.name() == null ? UNKNOWN_MEMBER_NAME : member.name();
    }
}
