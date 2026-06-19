package com.umc.product.notification.adapter.in.web.swagger;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.notification.adapter.in.web.dto.request.FcmRegistrationRequest;
import com.umc.product.notification.adapter.in.web.dto.request.FcmUnregistrationRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Notification | FCM", description = "FCM 관련 API")
public interface FcmControllerApi {

    @Operation(
        operationId = "FCM-001",
        summary = "FCM 토큰 등록",
        description = "사용자의 FCM 토큰을 등록하거나 업데이트합니다. 이미 등록된 토큰이 있으면 갱신됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "토큰 등록 성공"),
        @ApiResponse(responseCode = "404", description = "MEMBER-0001: 사용자를 찾을 수 없습니다."),
        @ApiResponse(responseCode = "500", description = "FCM-0004: FCM 토픽 구독에 실패했습니다.")
    })
    void refreshFcmToken(
        @Parameter(hidden = true)
        @CurrentMember MemberPrincipal memberPrincipal,
        FcmRegistrationRequest request
    );

    @Operation(
        operationId = "FCM-004",
        summary = "FCM 토큰 해제",
        description = "사용자의 특정 FCM 토큰을 비활성화합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "토큰 해제 성공")
    })
    void unregisterFcmToken(
        @Parameter(hidden = true)
        @CurrentMember MemberPrincipal memberPrincipal,
        FcmUnregistrationRequest request
    );

    @Operation(
        operationId = "FCM-002",
        summary = "기존 토픽 구독 해제",
        description = "FCM Topic에 실행 환경 관련 Prefix가 붙기 전에 구독중이던 모든 Legacy Topic을 구독 해제합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "레거시 토픽 구독 해제 성공")
    })
    void unsubscribeAllMemberLegacyTopics(
        @Parameter(hidden = true)
        @CurrentMember MemberPrincipal memberPrincipal
    );

    @Operation(
        operationId = "FCM-003",
        summary = "FCM 토픽 재구독",
        description = "요청 시점 기준으로 회원이 구독해야 하는 Topic들을 다시 구독처리합니다."
    )
    void resubscribeAllMemberLegacyTopics();
}
