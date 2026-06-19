package com.umc.product.notification.application.event;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.notification.application.port.in.dto.RequestFcmNotificationCommand;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record FcmNotificationRequestedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID requestId,
    Long requesterMemberId,
    List<Long> memberIds,
    Long targetGisuId,
    Long targetChapterId,
    Long targetSchoolId,
    Set<ChallengerPart> targetParts,
    String title,
    String body,
    Map<String, String> data,
    String imageUrl,
    String deepLink
) implements DomainEvent {

    public FcmNotificationRequestedEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        if (requestId == null) {
            requestId = UUID.randomUUID();
        }
        memberIds = normalizeMemberIds(memberIds);
        targetParts = targetParts == null ? Set.of() : Set.copyOf(targetParts);
        data = data == null ? Map.of() : Map.copyOf(data);
    }

    public static FcmNotificationRequestedEvent from(
        UUID requestId,
        Instant queuedAt,
        RequestFcmNotificationCommand command
    ) {
        return new FcmNotificationRequestedEvent(
            null,
            queuedAt,
            requestId,
            command.requesterMemberId(),
            command.memberIds(),
            command.targetGisuId(),
            command.targetChapterId(),
            command.targetSchoolId(),
            command.targetParts(),
            command.title(),
            command.body(),
            command.data(),
            command.imageUrl(),
            command.deepLink()
        );
    }

    @Override
    public String eventType() {
        return "notification.fcm.requested";
    }

    private static List<Long> normalizeMemberIds(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }
        return memberIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }
}
