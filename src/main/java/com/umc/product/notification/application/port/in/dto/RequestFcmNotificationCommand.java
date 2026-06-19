package com.umc.product.notification.application.port.in.dto;

import com.umc.product.common.domain.enums.ChallengerPart;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.Builder;

@Builder
public record RequestFcmNotificationCommand(
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
) {

    public RequestFcmNotificationCommand {
        memberIds = normalizeMemberIds(memberIds);
        targetParts = targetParts == null ? Set.of() : Set.copyOf(targetParts);
        data = data == null ? Map.of() : Map.copyOf(data);
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("FCM 알림 제목은 필수입니다.");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("FCM 알림 본문은 필수입니다.");
        }
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
