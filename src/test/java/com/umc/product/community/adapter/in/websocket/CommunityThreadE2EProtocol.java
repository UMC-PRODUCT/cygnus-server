package com.umc.product.community.adapter.in.websocket;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class CommunityThreadE2EProtocol {

    private CommunityThreadE2EProtocol() {
    }

    static String userEvents() {
        return "/user/queue/community/threads/events";
    }

    static String messages(Long threadId) {
        return "/app/community/threads/%d/messages".formatted(threadId);
    }

    static String edit(Long threadId, Long messageId) {
        return "/app/community/threads/%d/messages/%d/edit".formatted(threadId, messageId);
    }

    static String delete(Long threadId, Long messageId) {
        return "/app/community/threads/%d/messages/%d/delete".formatted(threadId, messageId);
    }

    static String reaction(Long threadId, Long messageId, String action) {
        return "/app/community/threads/%d/messages/%d/reactions/%s"
            .formatted(threadId, messageId, action);
    }

    static String read(Long threadId) {
        return "/app/community/threads/%d/read".formatted(threadId);
    }

    static Map<String, Object> createMessage(
        UUID clientMessageId,
        String content,
        List<Long> mentionedMemberIds,
        Long replyToId
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("clientMessageId", clientMessageId.toString());
        body.put("type", "TEXT");
        body.put("content", content);
        body.put("fileMetadataIds", List.of());
        body.put("mentionedMemberIds", mentionedMemberIds);
        if (replyToId != null) {
            body.put("replyToId", replyToId);
        }
        return body;
    }
}
