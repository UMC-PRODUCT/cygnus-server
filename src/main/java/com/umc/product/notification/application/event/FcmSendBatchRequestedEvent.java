package com.umc.product.notification.application.event;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.OutboxDispatchMode;

public record FcmSendBatchRequestedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID requestId,
    List<Long> tokenIds,
    String title,
    String body,
    Map<String, String> data,
    String imageUrl,
    String deepLink
) implements DomainEvent {

    public FcmSendBatchRequestedEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        if (requestId == null) {
            requestId = UUID.randomUUID();
        }
        tokenIds = normalizeTokenIds(tokenIds);
        data = data == null ? Map.of() : Map.copyOf(data);
    }

    public static FcmSendBatchRequestedEvent create(
        UUID requestId,
        List<Long> tokenIds,
        FcmNotificationRequestedEvent source
    ) {
        return new FcmSendBatchRequestedEvent(
            null,
            null,
            requestId,
            tokenIds,
            source.title(),
            source.body(),
            source.data(),
            source.imageUrl(),
            source.deepLink()
        );
    }

    public static FcmSendBatchRequestedEvent retry(
        List<Long> tokenIds,
        FcmSendBatchRequestedEvent source
    ) {
        return new FcmSendBatchRequestedEvent(
            null,
            null,
            source.requestId(),
            tokenIds,
            source.title(),
            source.body(),
            source.data(),
            source.imageUrl(),
            source.deepLink()
        );
    }

    @Override
    public String eventType() {
        return "notification.fcm.batch.requested";
    }

    @Override
    public OutboxDispatchMode outboxDispatchMode() {
        return OutboxDispatchMode.NON_TRANSACTIONAL;
    }

    private static List<Long> normalizeTokenIds(List<Long> tokenIds) {
        if (tokenIds == null || tokenIds.isEmpty()) {
            return List.of();
        }
        return tokenIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }
}
