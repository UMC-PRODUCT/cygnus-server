package com.umc.product.notification.adapter.in.web.dto.request;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.notification.application.port.in.dto.RequestFcmNotificationCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        Set<ChallengerPart> parts
    ) {
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
