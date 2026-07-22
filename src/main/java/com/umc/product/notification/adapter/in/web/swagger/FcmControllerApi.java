package com.umc.product.notification.adapter.in.web.swagger;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.notification.adapter.in.web.dto.request.FcmRegistrationRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Notification | FCM", description = "FCM 관련 API")
public interface FcmControllerApi {

    @Operation(
        operationId = "FCM-001",
        summary = "FCM installation 등록",
        description = "앱 installation의 현재 회원과 FCM 토큰을 등록하거나 갱신합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "토큰 등록 성공"),
        @ApiResponse(responseCode = "400", description = "installationId 또는 FCM 토큰 형식 오류")
    })
    void registerFcmInstallation(
        @Parameter(hidden = true)
        @CurrentMember MemberPrincipal memberPrincipal,
        FcmRegistrationRequest request
    );

    @Operation(
        operationId = "FCM-004",
        summary = "FCM installation 해제",
        description = "현재 회원이 소유한 앱 installation의 FCM 토큰을 비활성화합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "토큰 해제 성공")
    })
    void unregisterFcmInstallation(
        @Parameter(hidden = true)
        @CurrentMember MemberPrincipal memberPrincipal,
        @Parameter(description = "Firebase Installation ID")
        String installationId
    );
}
