package com.umc.product.notification.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.notification.adapter.in.graphql.dto.FcmInstallationGraphQlRequest;
import com.umc.product.notification.adapter.in.graphql.dto.FcmInstallationGraphQlResponse;
import com.umc.product.notification.adapter.in.graphql.dto.FcmNotificationGraphQlRequest;
import com.umc.product.notification.application.port.in.ManageFcmUseCase;
import com.umc.product.notification.application.port.in.RequestFcmNotificationUseCase;
import com.umc.product.notification.application.port.in.dto.FcmNotificationRequestInfo;
import com.umc.product.notification.application.port.in.dto.UnregisterFcmTokenCommand;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class NotificationGraphQlController {

    private final ManageFcmUseCase manageFcmUseCase;
    private final RequestFcmNotificationUseCase requestFcmNotificationUseCase;

    @MutationMapping
    public FcmInstallationGraphQlResponse.Installation registerFcmInstallation(
        @CurrentMember MemberPrincipal principal,
        @Argument FcmInstallationGraphQlRequest input
    ) {
        manageFcmUseCase.registerFcmToken(input.toCommand(principal.getMemberId()));
        return new FcmInstallationGraphQlResponse.Installation(input.installationId());
    }

    @MutationMapping
    public FcmInstallationGraphQlResponse.Deleted unregisterFcmInstallation(
        @CurrentMember MemberPrincipal principal,
        @Argument String installationId
    ) {
        manageFcmUseCase.unregisterFcmToken(
            UnregisterFcmTokenCommand.of(principal.getMemberId(), installationId)
        );
        return new FcmInstallationGraphQlResponse.Deleted(installationId);
    }

    @MutationMapping
    @CheckAccess(resourceType = ResourceType.FCM, permission = PermissionType.WRITE)
    @Audited(
        domain = Domain.FCM,
        action = AuditAction.CREATE,
        targetType = "FcmNotificationRequest",
        targetId = "#result.requestId()",
        description = "'관리자 FCM 알림 발송을 요청했습니다.'"
    )
    public FcmNotificationRequestInfo requestFcmNotification(
        @CurrentMember MemberPrincipal principal,
        @Argument FcmNotificationGraphQlRequest input
    ) {
        return requestFcmNotificationUseCase.request(input.toCommand(principal.getMemberId()));
    }
}
