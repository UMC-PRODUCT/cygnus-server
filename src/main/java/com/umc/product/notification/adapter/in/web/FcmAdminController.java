package com.umc.product.notification.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.notification.adapter.in.web.dto.request.FcmAdminSendRequest;
import com.umc.product.notification.adapter.in.web.dto.response.FcmAdminSendResponse;
import com.umc.product.notification.application.port.in.RequestFcmNotificationUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications/admin/fcm")
@RequiredArgsConstructor
@Tag(name = "Notification | FCM Admin", description = "운영진이 모바일 푸시 알림 발송을 요청합니다.")
public class FcmAdminController {

    private final RequestFcmNotificationUseCase requestFcmNotificationUseCase;

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(operationId = "FCM-ADMIN-001", summary = "관리자 FCM 알림 발송 요청")
    @CheckAccess(resourceType = ResourceType.FCM, permission = PermissionType.WRITE)
    @Audited(
        domain = Domain.FCM,
        action = AuditAction.CREATE,
        targetType = "FcmNotificationRequest",
        targetId = "#result.requestId()",
        description = "'관리자 FCM 알림 발송을 요청했습니다.'"
    )
    public FcmAdminSendResponse send(
        @CurrentMember MemberPrincipal memberPrincipal,
        @RequestBody @Valid FcmAdminSendRequest request
    ) {
        return FcmAdminSendResponse.from(
            requestFcmNotificationUseCase.request(request.toCommand(memberPrincipal.getMemberId()))
        );
    }
}
