package com.umc.product.inhouse.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.inhouse.adapter.in.web.dto.response.UmcProductMyProfileResponse;
import com.umc.product.inhouse.application.port.in.query.GetUmcProductMemberUseCase;
import com.umc.product.inhouse.application.service.UmcProductAccessPolicy;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/umc-product/members")
@RequiredArgsConstructor
@Tag(name = "Organization | UMC PRODUCT 내 프로필 Query", description = "로그인 계정에 연동된 UMC PRODUCT 프로필을 조회합니다.")
public class UmcProductMyProfileQueryController {

    private final GetUmcProductMemberUseCase getUmcProductMemberUseCase;
    private final UmcProductAccessPolicy umcProductAccessPolicy;

    @GetMapping("/me")
    @Operation(
        operationId = "UMC-PRODUCT-MEMBER-103",
        summary = "내 UMC PRODUCT 프로필 조회",
        description = "연동 프로필과 UMC PRODUCT 관리 가능 여부를 반환합니다. 연동 프로필이 없으면 profile은 null입니다."
    )
    public UmcProductMyProfileResponse getMyProfile(@CurrentMember MemberPrincipal memberPrincipal) {
        Long memberId = memberPrincipal.getMemberId();
        return UmcProductMyProfileResponse.of(
            getUmcProductMemberUseCase.findByAccountMemberId(memberId).orElse(null),
            umcProductAccessPolicy.canManageUmcProduct(memberId)
        );
    }
}
