package com.umc.product.notification.adapter.in.graphql.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.notification.application.port.in.dto.RequestFcmNotificationCommand;

public record FcmNotificationGraphQlRequest(
    Target target,
    Message message
) {

    public RequestFcmNotificationCommand toCommand(Long requesterMemberId) {
        if (target == null || !target.hasAnyTarget()) {
            throw new IllegalArgumentException("FCM 알림 발송 대상은 하나 이상 지정해야 합니다.");
        }
        if (message == null) {
            throw new IllegalArgumentException("FCM 알림 메시지는 필수입니다.");
        }
        return RequestFcmNotificationCommand.builder()
            .requesterMemberId(requesterMemberId)
            .memberIds(target.memberIds())
            .targetGisuId(target.gisuId())
            .targetChapterId(target.chapterId())
            .targetSchoolId(target.schoolId())
            .targetParts(target.parts())
            .title(message.title())
            .body(message.body())
            .data(message.dataMap())
            .imageUrl(message.imageUrl())
            .deepLink(message.deepLink())
            .build();
    }

    public record Target(
        List<Long> memberIds,
        Long gisuId,
        Long chapterId,
        Long schoolId,
        Set<ChallengerPart> parts
    ) {

        private boolean hasAnyTarget() {
            return memberIds != null && !memberIds.isEmpty()
                || gisuId != null
                || chapterId != null
                || schoolId != null
                || parts != null && !parts.isEmpty();
        }
    }

    public record Message(
        String title,
        String body,
        List<KeyValue> data,
        String imageUrl,
        String deepLink
    ) {

        private Map<String, String> dataMap() {
            if (data == null || data.isEmpty()) {
                return Map.of();
            }
            Map<String, String> result = new LinkedHashMap<>();
            data.forEach(item -> result.put(item.key(), item.value()));
            return Map.copyOf(result);
        }
    }

    public record KeyValue(String key, String value) {
    }
}
