package com.umc.product.community.adapter.in.websocket;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.product.community.adapter.in.websocket.CommunityThreadStompProbe.StompFrame;

final class CommunityThreadFrameAwaiter {

    private final ObjectMapper objectMapper;
    private final Map<BlockingQueue<StompFrame>, Deque<JsonNode>> pending = new IdentityHashMap<>();

    CommunityThreadFrameAwaiter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    JsonNode awaitType(BlockingQueue<StompFrame> frames, String type, Duration timeout)
        throws Exception {
        return await(
            frames,
            event -> type.equals(event.path("type").asText()),
            "type=" + type,
            timeout
        );
    }

    JsonNode awaitMessage(
        BlockingQueue<StompFrame> frames,
        String type,
        String content,
        Duration timeout
    ) throws Exception {
        return await(
            frames,
            event -> type.equals(event.path("type").asText())
                && content.equals(event.at("/payload/message/content").asText()),
            "type=" + type + ", content=" + content,
            timeout
        );
    }

    JsonNode awaitMessage(
        BlockingQueue<StompFrame> frames,
        String type,
        Long messageId,
        Duration timeout
    ) throws Exception {
        return await(
            frames,
            event -> type.equals(event.path("type").asText())
                && messageId.equals(event.at("/payload/message/messageId").asLong()),
            "type=" + type + ", messageId=" + messageId,
            timeout
        );
    }

    JsonNode awaitAck(BlockingQueue<StompFrame> frames, UUID commandId, Duration timeout)
        throws Exception {
        return await(
            frames,
            event -> "command.acknowledged".equals(event.path("type").asText())
                && commandId.toString().equals(event.at("/payload/commandId").asText()),
            "ACK commandId=" + commandId,
            timeout
        );
    }

    JsonNode awaitError(BlockingQueue<StompFrame> frames, UUID commandId, Duration timeout)
        throws Exception {
        return await(
            frames,
            error -> commandId.toString().equals(error.path("commandId").asText()),
            "error commandId=" + commandId,
            timeout
        );
    }

    JsonNode await(
        BlockingQueue<StompFrame> frames,
        Predicate<JsonNode> predicate,
        String description,
        Duration timeout
    ) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        List<String> observed = new ArrayList<>();
        JsonNode buffered = removeFirstMatching(frames, predicate, observed);
        if (buffered != null) {
            return buffered;
        }
        while (true) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                throw timeout(description, observed);
            }
            StompFrame frame = frames.poll(remaining, TimeUnit.NANOSECONDS);
            if (frame == null) {
                throw timeout(description, observed);
            }
            JsonNode payload = objectMapper.readTree(frame.payload());
            if (predicate.test(payload)) {
                return payload;
            }
            pending(frames).addLast(payload);
            observed.add(frame.payload());
        }
    }

    void assertNoType(BlockingQueue<StompFrame> frames, String type, Duration window)
        throws Exception {
        long deadline = System.nanoTime() + window.toNanos();
        assertPendingDoesNotContain(frames, type);
        while (true) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                return;
            }
            StompFrame frame = frames.poll(remaining, TimeUnit.NANOSECONDS);
            if (frame == null) {
                return;
            }
            JsonNode payload = objectMapper.readTree(frame.payload());
            if (type.equals(payload.path("type").asText())) {
                throw new AssertionError("예상하지 않은 STOMP event를 받았습니다: " + frame.payload());
            }
            pending(frames).addLast(payload);
        }
    }

    private JsonNode removeFirstMatching(
        BlockingQueue<StompFrame> frames,
        Predicate<JsonNode> predicate,
        List<String> observed
    ) {
        Deque<JsonNode> buffered = pending(frames);
        for (var iterator = buffered.iterator(); iterator.hasNext();) {
            JsonNode payload = iterator.next();
            if (predicate.test(payload)) {
                iterator.remove();
                return payload;
            }
            observed.add(payload.toString());
        }
        return null;
    }

    private void assertPendingDoesNotContain(BlockingQueue<StompFrame> frames, String type) {
        pending(frames).stream()
            .filter(payload -> type.equals(payload.path("type").asText()))
            .findFirst()
            .ifPresent(payload -> {
                throw new AssertionError("예상하지 않은 STOMP event를 받았습니다: " + payload);
            });
    }

    private Deque<JsonNode> pending(BlockingQueue<StompFrame> frames) {
        return pending.computeIfAbsent(frames, ignored -> new ArrayDeque<>());
    }

    private AssertionError timeout(String description, List<String> observed) {
        return new AssertionError(
            "STOMP frame을 제한 시간 안에 받지 못했습니다: " + description + ", observed=" + observed
        );
    }
}
