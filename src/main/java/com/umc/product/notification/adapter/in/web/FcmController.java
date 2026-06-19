package com.umc.product.notification.adapter.in.web;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.notification.adapter.in.web.dto.request.FcmRegistrationRequest;
import com.umc.product.notification.adapter.in.web.dto.request.FcmUnregistrationRequest;
import com.umc.product.notification.adapter.in.web.swagger.FcmControllerApi;
import com.umc.product.notification.application.port.in.ManageFcmTopicUseCase;
import com.umc.product.notification.application.port.in.ManageFcmUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FcmController implements FcmControllerApi {

    private final ManageFcmUseCase manageFcmUseCase;
    private final ManageFcmTopicUseCase manageFcmTopicUseCase;

    @Override
    @PutMapping({"/api/v1/notifications/fcm/tokens", "/api/v1/notification/fcm/token"})
    public void refreshFcmToken(
        @CurrentMember MemberPrincipal memberPrincipal,
        @RequestBody @Valid FcmRegistrationRequest request) {
        manageFcmUseCase.registerFcmToken(request.toCommand(memberPrincipal.getMemberId()));
    }

    @Override
    @DeleteMapping({"/api/v1/notifications/fcm/tokens", "/api/v1/notification/fcm/token"})
    public void unregisterFcmToken(
        @CurrentMember MemberPrincipal memberPrincipal,
        @RequestBody @Valid FcmUnregistrationRequest request) {
        manageFcmUseCase.unregisterFcmToken(request.toCommand(memberPrincipal.getMemberId()));
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
