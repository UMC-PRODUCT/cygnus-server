package com.umc.product.notification.adapter.in.web;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.notification.adapter.in.web.dto.request.FcmRegistrationRequest;
import com.umc.product.notification.adapter.in.web.swagger.FcmControllerApi;
import com.umc.product.notification.application.port.in.ManageFcmTopicUseCase;
import com.umc.product.notification.application.port.in.ManageFcmUseCase;
import com.umc.product.notification.application.port.in.dto.UnregisterFcmTokenCommand;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FcmController implements FcmControllerApi {

    private final ManageFcmUseCase manageFcmUseCase;
    private final ManageFcmTopicUseCase manageFcmTopicUseCase;

    @Override
    @PostMapping("/api/v1/notifications/fcm/installations")
    public void registerFcmInstallation(
        @CurrentMember MemberPrincipal memberPrincipal,
        @RequestBody @Valid FcmRegistrationRequest request) {
        manageFcmUseCase.registerFcmToken(request.toCommand(memberPrincipal.getMemberId()));
    }

    @Override
    @DeleteMapping("/api/v1/notifications/fcm/installations/{installationId}")
    public void unregisterFcmInstallation(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable String installationId) {
        manageFcmUseCase.unregisterFcmToken(
            UnregisterFcmTokenCommand.of(memberPrincipal.getMemberId(), installationId)
        );
    }

    @Override
    @DeleteMapping("/api/v1/notification/fcm/topics/legacy")
    public void unsubscribeAllMemberLegacyTopics(@CurrentMember MemberPrincipal memberPrincipal) {
        manageFcmTopicUseCase.unsubscribeLegacyTopics(memberPrincipal.getMemberId());
    }

    @Override
    public void resubscribeAllMemberLegacyTopics() {
        manageFcmTopicUseCase.resubscribeAllLegacyTopics();
    }


}
