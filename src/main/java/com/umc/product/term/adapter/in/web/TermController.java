package com.umc.product.term.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.global.security.annotation.Public;
import com.umc.product.term.adapter.in.web.dto.request.CreateTermAgreementRequest;
import com.umc.product.term.adapter.in.web.dto.request.CreateTermRequest;
import com.umc.product.term.adapter.in.web.dto.response.ActiveTermsResponse;
import com.umc.product.term.adapter.in.web.dto.response.RequiredTermConsentStatusResponse;
import com.umc.product.term.adapter.in.web.dto.response.TermResponse;
import com.umc.product.term.application.port.in.command.ManageTermAgreementUseCase;
import com.umc.product.term.application.port.in.command.ManageTermUseCase;
import com.umc.product.term.application.port.in.command.dto.CreateTermCommand;
import com.umc.product.term.application.port.in.command.dto.CreateTermConsentCommand;
import com.umc.product.term.application.port.in.query.GetRequiredTermConsentStatusUseCase;
import com.umc.product.term.application.port.in.query.GetTermUseCase;
import com.umc.product.term.domain.enums.TermType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/v1/terms")
@Tag(name = "Terms | 약관", description = "약관을 조회하고 생성합니다.")
public class TermController {

    private final GetTermUseCase getTermUseCase;
    private final GetRequiredTermConsentStatusUseCase getRequiredTermConsentStatusUseCase;
    private final ManageTermUseCase manageTermUseCase;
    private final ManageTermAgreementUseCase manageTermAgreementUseCase;

    @GetMapping
    @Public
    @Operation(operationId = "TERM-104", summary = "활성 약관 전체 조회")
    ActiveTermsResponse getActiveTerms() {
        return ActiveTermsResponse.from(getTermUseCase.listActiveTerms());
    }

    @GetMapping("type/{termType}")
    @Public
    @Operation(operationId = "TERM-101", summary = "약관 유형으로 약관 조회")
    TermResponse getTerms(@PathVariable TermType termType) {
        return TermResponse.from(getTermUseCase.getTermsByType(termType));
    }

    @GetMapping("{termsId}")
    @Public
    @Operation(operationId = "TERM-102", summary = "약관 ID로 약관 조회")
    TermResponse getTermsById(@PathVariable Long termsId) {
        return TermResponse.from(getTermUseCase.getTermsById(termsId));
    }

    @GetMapping("consent-status/me")
    @Operation(operationId = "TERM-103", summary = "내 필수 약관 재동의 상태 조회")
    RequiredTermConsentStatusResponse getMyRequiredTermConsentStatus(@CurrentMember MemberPrincipal memberPrincipal) {
        return RequiredTermConsentStatusResponse.from(
            getRequiredTermConsentStatusUseCase.getRequiredTermConsentStatus(memberPrincipal.getMemberId())
        );
    }

    @PostMapping("agreements")
    @Operation(summary = "[TERM-002] 내 약관 동의 저장")
    void createMyTermAgreement(
        @CurrentMember MemberPrincipal memberPrincipal,
        @Valid @RequestBody CreateTermAgreementRequest request
    ) {
        Long memberId = memberPrincipal.getMemberId();
        manageTermAgreementUseCase.createTermConsent(
            CreateTermConsentCommand.builder()
                .memberId(memberId)
                .termId(request.termsId())
                .isAgreed(request.isAgreed())
                .build()
        );
    }

    @PostMapping
    @Operation(operationId = "TERM-001", summary = "약관 생성", description = "새 약관을 생성하면 기존 약관은 비활성화됩니다.")
    @CheckAccess(
        resourceType = ResourceType.TERM,
        permission = PermissionType.WRITE
    )
    Long createTerms(@Valid @RequestBody CreateTermRequest request) {
        return manageTermUseCase.createTerms(
            CreateTermCommand.builder()
                .link(request.link())
                .required(request.isMandatory())
                .type(request.termType())
                .build()
        );
    }
}
