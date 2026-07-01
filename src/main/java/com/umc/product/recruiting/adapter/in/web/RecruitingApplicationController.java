package com.umc.product.recruiting.adapter.in.web;

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
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingApplicationResponse;
import com.umc.product.recruiting.application.port.in.command.CancelRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingApplicationDraftUseCase;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recruiting/applications")
@RequiredArgsConstructor
public class RecruitingApplicationController {

    private final CreateRecruitingApplicationDraftUseCase createDraftUseCase;
    private final UpdateRecruitingApplicationDraftUseCase updateDraftUseCase;
    private final SubmitRecruitingApplicationUseCase submitUseCase;
    private final CancelRecruitingApplicationUseCase cancelUseCase;

    @PostMapping
    public RecruitingApplicationResponse createDraft(
        @CurrentMember MemberPrincipal memberPrincipal,
        @Valid @RequestBody CreateRecruitingApplicationDraftRequest request
    ) {
        return RecruitingApplicationResponse.from(
            createDraftUseCase.createDraft(request.toCommand(
                resolveMemberId(memberPrincipal, request.applicantMemberId())
            ))
        );
    }

    @PutMapping("/{applicationId}")
    public RecruitingApplicationResponse updateDraft(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long applicationId,
        @Valid @RequestBody UpdateRecruitingApplicationDraftRequest request
    ) {
        return RecruitingApplicationResponse.from(
            updateDraftUseCase.updateDraft(request.toCommand(
                applicationId,
                resolveMemberId(memberPrincipal, request.requesterMemberId())
            ))
        );
    }

    @PostMapping("/{applicationId}/submit")
    public RecruitingApplicationResponse submit(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long applicationId,
        @RequestBody(required = false) SubmitRecruitingApplicationRequest request,
        HttpServletRequest servletRequest
    ) {
        SubmitRecruitingApplicationRequest actualRequest = request == null
            ? new SubmitRecruitingApplicationRequest(null, null)
            : request;
        return RecruitingApplicationResponse.from(
            submitUseCase.submit(actualRequest.toCommand(
                applicationId,
                resolveMemberId(memberPrincipal, actualRequest.requesterMemberId()),
                servletRequest.getRemoteAddr()
            ))
        );
    }

    @PatchMapping("/{applicationId}/cancel")
    public RecruitingApplicationResponse cancel(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long applicationId,
        @RequestBody(required = false) CancelRecruitingApplicationRequest request
    ) {
        CancelRecruitingApplicationRequest actualRequest = request == null
            ? new CancelRecruitingApplicationRequest(null, null)
            : request;
        return RecruitingApplicationResponse.from(
            cancelUseCase.cancel(actualRequest.toCommand(
                applicationId,
                resolveMemberId(memberPrincipal, actualRequest.requesterMemberId())
            ))
        );
    }

    private Long resolveMemberId(MemberPrincipal memberPrincipal, Long requestMemberId) {
        if (memberPrincipal != null) {
            return memberPrincipal.getMemberId();
        }
        return requestMemberId;
    }
}
