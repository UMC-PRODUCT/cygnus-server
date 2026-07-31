package com.umc.product.recruiting.adapter.in.web;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.web.dto.request.CancelRecruitingApplicationRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.CreateRecruitingApplicationDraftRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.SubmitRecruitingApplicationRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.UpdateRecruitingApplicationDraftRequest;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingApplicationCreatedResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingApplicationResponse;
import com.umc.product.recruiting.application.port.in.command.CancelRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingApplicationDraftUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recruiting/applications")
@Validated
@Tag(name = "Recruiting | 지원서", description = "지원서 초안 작성, 수정, 제출, 철회를 처리합니다.")
@RequiredArgsConstructor
public class RecruitingApplicationController {

    private final CreateRecruitingApplicationDraftUseCase createDraftUseCase;
    private final UpdateRecruitingApplicationDraftUseCase updateDraftUseCase;
    private final SubmitRecruitingApplicationUseCase submitUseCase;
    private final CancelRecruitingApplicationUseCase cancelUseCase;

    @PostMapping
    @Operation(
        operationId = "RECRUITING-APPLICATION-001",
        summary = "지원서 초안 생성",
        description = "로그인 회원이 지원 폼에 대한 지원서 초안을 생성합니다."
    )
    public RecruitingApplicationCreatedResponse createDraft(
        @Parameter(hidden = true)
        @CurrentMember MemberPrincipal memberPrincipal,
        @Valid @RequestBody CreateRecruitingApplicationDraftRequest request
    ) {
        return RecruitingApplicationCreatedResponse.from(
            createDraftUseCase.createDraft(request.toCommand(resolveMemberId(memberPrincipal)))
        );
    }

    @PutMapping("/{applicationId}")
    @Operation(
        operationId = "RECRUITING-APPLICATION-002",
        summary = "지원서 초안 수정",
        description = "로그인 회원이 본인의 지원 기본 정보와 답변을 저장합니다."
    )
    public RecruitingApplicationResponse updateDraft(
        @Parameter(hidden = true)
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long applicationId,
        @Valid @RequestBody UpdateRecruitingApplicationDraftRequest request
    ) {
        return RecruitingApplicationResponse.from(
            updateDraftUseCase.updateDraft(request.toCommand(
                applicationId,
                resolveMemberId(memberPrincipal)
            ))
        );
    }

    @PostMapping("/{applicationId}/submit")
    @Operation(
        operationId = "RECRUITING-APPLICATION-003",
        summary = "지원서 제출",
        description = "작성 중인 지원서를 최종 제출 상태로 변경합니다. 제출 IP는 요청 본문 값이 없으면 서버가 확인한 원격 주소를 사용합니다."
    )
    public RecruitingApplicationResponse submit(
        @Parameter(hidden = true)
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long applicationId,
        @RequestBody(required = false) SubmitRecruitingApplicationRequest request,
        HttpServletRequest servletRequest
    ) {
        SubmitRecruitingApplicationRequest actualRequest = request == null
            ? new SubmitRecruitingApplicationRequest(null)
            : request;
        return RecruitingApplicationResponse.from(
            submitUseCase.submit(actualRequest.toCommand(
                applicationId,
                resolveMemberId(memberPrincipal),
                servletRequest.getRemoteAddr()
            ))
        );
    }

    @PatchMapping("/{applicationId}/cancel")
    @Operation(
        operationId = "RECRUITING-APPLICATION-004",
        summary = "지원서 철회",
        description = "제출 전후의 지원서를 지원자 요청에 따라 철회합니다."
    )
    public RecruitingApplicationResponse cancel(
        @Parameter(hidden = true)
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long applicationId,
        @RequestBody(required = false) CancelRecruitingApplicationRequest request
    ) {
        CancelRecruitingApplicationRequest actualRequest = request == null
            ? new CancelRecruitingApplicationRequest(null)
            : request;
        return RecruitingApplicationResponse.from(
            cancelUseCase.cancel(actualRequest.toCommand(
                applicationId,
                resolveMemberId(memberPrincipal)
            ))
        );
    }

    private Long resolveMemberId(MemberPrincipal memberPrincipal) {
        return memberPrincipal.getMemberId();
    }
}
