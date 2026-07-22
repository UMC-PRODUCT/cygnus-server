package com.umc.product.chat.application.service.query;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageReplyInfo;
import com.umc.product.chat.application.port.in.query.dto.ChatReactionInfo;
import com.umc.product.chat.application.port.out.LoadChatMessageMentionPort;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.LoadChatMessageReactionPort;
import com.umc.product.chat.application.port.out.dto.ChatReactionSummary;
import com.umc.product.chat.domain.ChatMessage;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatMessageInfoAssembler {

    private static final int REPLY_SNIPPET_CODE_POINTS = 100;

    private final LoadChatMessagePort loadChatMessagePort;
    private final LoadChatMessageMentionPort loadChatMessageMentionPort;
    private final LoadChatMessageReactionPort loadChatMessageReactionPort;

    public ChatMessageInfo assemble(ChatMessage message, Long viewerMemberId) {
        return assemble(List.of(message), viewerMemberId).get(0);
    }

    public List<ChatMessageInfo> assemble(List<ChatMessage> messages, Long viewerMemberId) {
        if (messages.isEmpty()) {
            return List.of();
        }

        List<Long> messageIds = messages.stream().map(ChatMessage::getId).toList();
        Map<Long, List<Long>> mentionsByMessage = loadChatMessageMentionPort
            .listMemberIdsByMessageIds(messageIds);
        Map<Long, List<ChatReactionInfo>> reactionsByMessage = groupReactions(
            loadChatMessageReactionPort.summarizeByMessageIds(messageIds, viewerMemberId)
        );
        Map<Long, ChatMessage> repliesById = loadReplies(messages);

        return messages.stream()
            .map(message -> toInfo(
                message,
                mentionsByMessage.getOrDefault(message.getId(), List.of()),
                reactionsByMessage.getOrDefault(message.getId(), List.of()),
                message.getReplyToMessageId() == null
                    ? null
                    : repliesById.get(message.getReplyToMessageId())
            ))
            .toList();
    }

    public Map<Long, ChatMessageInfo> assembleForViewers(
        ChatMessage message,
        List<Long> viewerMemberIds
    ) {
        if (viewerMemberIds.isEmpty()) {
            return Map.of();
        }

        List<Long> messageIds = List.of(message.getId());
        List<Long> mentionedMemberIds = loadChatMessageMentionPort
            .listMemberIdsByMessageIds(messageIds)
            .getOrDefault(message.getId(), List.of());
        Map<Long, List<ChatReactionSummary>> reactionsByViewer = loadChatMessageReactionPort
            .summarizeByMessageIdsForViewers(messageIds, viewerMemberIds);
        ChatMessage reply = message.getReplyToMessageId() == null
            ? null
            : loadReplies(List.of(message)).get(message.getReplyToMessageId());

        Map<Long, ChatMessageInfo> result = new LinkedHashMap<>();
        for (Long viewerMemberId : viewerMemberIds) {
            Map<Long, List<ChatReactionInfo>> reactionsByMessage = groupReactions(
                reactionsByViewer.getOrDefault(viewerMemberId, List.of())
            );
            result.put(
                viewerMemberId,
                toInfo(
                    message,
                    mentionedMemberIds,
                    reactionsByMessage.getOrDefault(message.getId(), List.of()),
                    reply
                )
            );
        }
        return Collections.unmodifiableMap(result);
    }

    private Map<Long, List<ChatReactionInfo>> groupReactions(List<ChatReactionSummary> summaries) {
        return summaries.stream().collect(Collectors.groupingBy(
            ChatReactionSummary::messageId,
            LinkedHashMap::new,
            Collectors.mapping(
                summary -> new ChatReactionInfo(
                    summary.emoji(),
                    summary.count(),
                    summary.reactedByViewer()
                ),
                Collectors.toList()
            )
        ));
    }

    private Map<Long, ChatMessage> loadReplies(List<ChatMessage> messages) {
        List<Long> replyIds = messages.stream()
            .map(ChatMessage::getReplyToMessageId)
            .filter(id -> id != null)
            .distinct()
            .toList();
        if (replyIds.isEmpty()) {
            return Map.of();
        }
        return loadChatMessagePort.listByIds(replyIds).stream()
            .collect(Collectors.toMap(ChatMessage::getId, Function.identity()));
    }

    private ChatMessageInfo toInfo(
        ChatMessage message,
        List<Long> mentionedMemberIds,
        List<ChatReactionInfo> reactions,
        ChatMessage reply
    ) {
        return new ChatMessageInfo(
            message.getId(),
            message.getRoomId(),
            message.getSenderMemberId(),
            message.getContentType(),
            message.getContent(),
            message.getFileMetadataIds(),
            message.getCreatedAt(),
            message.getReplyToMessageId(),
            message.getClientMessageId(),
            message.getEditedAt(),
            message.getDeletedAt(),
            mentionedMemberIds,
            toReplyInfo(reply),
            reactions
        );
    }

    private ChatMessageReplyInfo toReplyInfo(ChatMessage reply) {
        if (reply == null) {
            return null;
        }
        return new ChatMessageReplyInfo(
            reply.getId(),
            reply.getSenderMemberId(),
            truncate(reply.getContent())
        );
    }

    private String truncate(String content) {
        if (content == null) {
            return null;
        }
        int codePoints = content.codePointCount(0, content.length());
        if (codePoints <= REPLY_SNIPPET_CODE_POINTS) {
            return content;
        }
        int end = content.offsetByCodePoints(0, REPLY_SNIPPET_CODE_POINTS);
        return content.substring(0, end);
    }
}
