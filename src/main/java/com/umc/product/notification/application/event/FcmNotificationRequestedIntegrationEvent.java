package com.umc.product.notification.application.event;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.umc.product.global.event.domain.IntegrationEvent;

public record FcmNotificationRequestedIntegrationEvent(
    UUID eventId,
    Instant occurredAt,
    UUID requestId,
    Detail detail
) implements IntegrationEvent {

    private static final String EVENT_TYPE = "notification.fcm.requested.v1";
    private static final String SOURCE = "umc-product.notification-gateway";

    public FcmNotificationRequestedIntegrationEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        if (requestId == null) {
            throw new IllegalArgumentException("FCM integration event requestId는 필수입니다.");
        }
        if (detail == null) {
            throw new IllegalArgumentException("FCM integration event detail은 필수입니다.");
        }
    }

    public static FcmNotificationRequestedIntegrationEvent create(
        FcmNotificationRequestedEvent source,
        List<Long> memberIds,
        int chunkIndex,
        int chunkCount
    ) {
        return new FcmNotificationRequestedIntegrationEvent(
            null,
            source.occurredAt(),
            source.requestId(),
            new Detail(
                chunkIndex,
                chunkCount,
                memberIds,
                source.title(),
                source.body(),
                source.data(),
                source.imageUrl(),
                source.deepLink()
            )
        );
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    @Override
    public String source() {
        return SOURCE;
    }

    public record Detail(
        int chunkIndex,
        int chunkCount,
        List<Long> memberIds,
        String title,
        String body,
        Map<String, String> data,
        String imageUrl,
        String deepLink
    ) {

        public Detail {
            if (chunkIndex < 0 || chunkCount < 1 || chunkIndex >= chunkCount) {
                throw new IllegalArgumentException("FCM integration event chunk 정보가 올바르지 않습니다.");
            }
            memberIds = memberIds == null ? List.of() : List.copyOf(memberIds);
            if (memberIds.size() > 500) {
                throw new IllegalArgumentException("FCM integration event 대상은 chunk당 500명을 초과할 수 없습니다.");
            }
            data = data == null ? Map.of() : Map.copyOf(data);
        }
    }
}
