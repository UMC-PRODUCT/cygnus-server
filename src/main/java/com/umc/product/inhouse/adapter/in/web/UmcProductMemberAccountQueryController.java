package com.umc.product.inhouse.adapter.in.web;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.inhouse.adapter.in.web.dto.request.FindUmcProductAccountCandidateRequest;
import com.umc.product.inhouse.adapter.in.web.dto.response.UmcProductAccountCandidateResponse;
import com.umc.product.inhouse.adapter.in.web.dto.response.UmcProductMemberAccountResponse;
import com.umc.product.inhouse.application.port.in.query.GetUmcProductMemberAccountUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/umc-product/members")
@RequiredArgsConstructor
@Tag(name = "Inhouse | UMC PRODUCT 계정 Query", description = "UMC PRODUCT 인원에 연동할 로그인 계정을 조회합니다.")
public class UmcProductMemberAccountQueryController {

    private final GetUmcProductMemberAccountUseCase getUmcProductMemberAccountUseCase;

    @GetMapping("/{umcProductMemberId}/accounts")
    @Operation(operationId = "UMC-PRODUCT-MEMBER-202", summary = "UMC PRODUCT 인원의 연동 계정 목록 조회")
    public List<UmcProductMemberAccountResponse> listAccounts(
        @PathVariable Long umcProductMemberId,
        @CurrentMember MemberPrincipal currentMember
    ) {
        return getUmcProductMemberAccountUseCase.listAccounts(
            currentMemberId(currentMember),
            umcProductMemberId
        ).stream().map(UmcProductMemberAccountResponse::from).toList();
    }

    @PostMapping("/account-candidates/search")
    @Operation(operationId = "UMC-PRODUCT-MEMBER-203", summary = "연동할 로그인 계정을 이메일로 정확히 검색")
    public UmcProductAccountCandidateResponse findCandidate(
        @CurrentMember MemberPrincipal currentMember,
        @RequestBody @Valid FindUmcProductAccountCandidateRequest request
    ) {
        return getUmcProductMemberAccountUseCase.findCandidateByEmail(
            currentMemberId(currentMember),
            request.email()
        ).map(UmcProductAccountCandidateResponse::from).orElse(null);
    }

    private Long currentMemberId(MemberPrincipal currentMember) {
        if (currentMember == null) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }
        return currentMember.getMemberId();
    }
}
