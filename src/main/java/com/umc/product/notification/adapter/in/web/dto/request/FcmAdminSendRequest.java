package com.umc.product.notification.adapter.in.web.dto.request;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.notification.application.port.in.dto.RequestFcmNotificationCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FcmAdminSendRequest(
    @NotNull @Valid Target target,
    @NotNull @Valid Message message
) {

    public RequestFcmNotificationCommand toCommand(Long requesterMemberId) {
        return RequestFcmNotificationCommand.builder()
            .requesterMemberId(requesterMemberId)
            .memberIds(target.memberIds())
            .targetGisuId(target.gisuId())
            .targetChapterId(target.chapterId())
            .targetSchoolId(target.schoolId())
            .targetParts(target.parts())
            .title(message.title())
            .body(message.body())
            .data(message.data())
            .imageUrl(message.imageUrl())
            .deepLink(message.deepLink())
            .build();
    }

    public record Target(
        List<Long> memberIds,
        Long gisuId,
        Long chapterId,
        Long schoolId,
        Set<@NotNull ChallengerPart> parts
    ) {

        @AssertTrue(message = "FCM 알림 발송 대상은 하나 이상 지정해야 합니다.") public boolean hasAnyTarget() {
            return hasMemberTarget() || gisuId != null || chapterId != null || schoolId != null || hasPartTarget();
        }

        private boolean hasMemberTarget() {
            return memberIds != null && memberIds.stream().anyMatch(memberId -> memberId != null);
        }

        private boolean hasPartTarget() {
            return parts != null && parts.stream().anyMatch(part -> part != null);
        }
    }

    public record Message(
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 500) String body,
        Map<@Size(max = 50) String, @Size(max = 500) String> data,
        @Size(max = 500) String imageUrl,
        @Size(max = 500) String deepLink
    ) {
    }
}
