package com.umc.product.recruiting.adapter.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import com.umc.product.recruiting.adapter.in.web.dto.request.AssignRecruitingInterviewRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.FindRecruitingInterviewScheduleCandidatesRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.SaveRecruitingInterviewEvaluationRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.SendRecruitingInterviewGuideRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.SkipRecruitingInterviewRequest;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingIdResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingInterviewEvaluationResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingInterviewScheduleCandidateResponse;
import com.umc.product.recruiting.application.port.in.command.AssignRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.command.FindRecruitingInterviewScheduleCandidatesUseCase;
import com.umc.product.recruiting.application.port.in.command.SaveRecruitingInterviewEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.command.SendRecruitingInterviewGuideUseCase;
import com.umc.product.recruiting.application.port.in.command.SkipRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingInterviewEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewEvaluationUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/recruiting")
@RequiredArgsConstructor
public class RecruitingAdminInterviewController {

    private final AssignRecruitingInterviewUseCase assignInterviewUseCase;
    private final SkipRecruitingInterviewUseCase skipInterviewUseCase;
    private final FindRecruitingInterviewScheduleCandidatesUseCase findScheduleCandidatesUseCase;
    private final SendRecruitingInterviewGuideUseCase sendInterviewGuideUseCase;
    private final SaveRecruitingInterviewEvaluationUseCase saveEvaluationUseCase;
    private final SubmitRecruitingInterviewEvaluationUseCase submitEvaluationUseCase;
    private final GetRecruitingInterviewEvaluationUseCase getEvaluationUseCase;

    @PostMapping("/seasons/{seasonId}/applications/{applicationId}/interviews")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    public RecruitingIdResponse assignInterview(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long seasonId,
        @PathVariable Long applicationId,
        @Valid @RequestBody AssignRecruitingInterviewRequest request
    ) {
        return RecruitingIdResponse.from(assignInterviewUseCase.assign(
            request.toCommand(applicationId, memberId(memberPrincipal))
        ));
    }

    @PostMapping("/seasons/{seasonId}/applications/{applicationId}/interviews/skip")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    public void skipInterview(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long seasonId,
        @PathVariable Long applicationId,
        @RequestBody(required = false) SkipRecruitingInterviewRequest request
    ) {
        SkipRecruitingInterviewRequest actualRequest = request == null
            ? new SkipRecruitingInterviewRequest(null)
            : request;
        skipInterviewUseCase.skip(actualRequest.toCommand(applicationId, memberId(memberPrincipal)));
    }

    @PostMapping("/seasons/{seasonId}/interviews/schedule-candidates")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.READ)
    public List<RecruitingInterviewScheduleCandidateResponse> findScheduleCandidates(
        @PathVariable Long seasonId,
        @Valid @RequestBody FindRecruitingInterviewScheduleCandidatesRequest request
    ) {
        return findScheduleCandidatesUseCase.findScheduleCandidates(request.toCommand())
            .stream()
            .map(RecruitingInterviewScheduleCandidateResponse::from)
            .toList();
    }

    @PostMapping("/seasons/{seasonId}/applications/{applicationId}/interview-guide")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    public void sendInterviewGuide(
        @PathVariable Long seasonId,
        @PathVariable Long applicationId,
        @Valid @RequestBody SendRecruitingInterviewGuideRequest request
    ) {
        sendInterviewGuideUseCase.sendGuide(request.toCommand(applicationId));
    }

    @PatchMapping("/seasons/{seasonId}/interview-assignments/{assignmentId}/evaluation")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    public void saveEvaluation(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long seasonId,
        @PathVariable Long assignmentId,
        @Valid @RequestBody SaveRecruitingInterviewEvaluationRequest request
    ) {
        saveEvaluationUseCase.saveEvaluation(request.toSaveCommand(assignmentId, memberId(memberPrincipal)));
    }

    @PostMapping("/seasons/{seasonId}/interview-assignments/{assignmentId}/evaluation/submit")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    public void submitEvaluation(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long seasonId,
        @PathVariable Long assignmentId,
        @Valid @RequestBody SaveRecruitingInterviewEvaluationRequest request
    ) {
        submitEvaluationUseCase.submitEvaluation(request.toSubmitCommand(assignmentId, memberId(memberPrincipal)));
    }

    @GetMapping("/seasons/{seasonId}/applications/{applicationId}/interview-evaluations")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.READ)
    public List<RecruitingInterviewEvaluationResponse> listVisibleEvaluations(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long seasonId,
        @PathVariable Long applicationId
    ) {
        return getEvaluationUseCase.listVisibleEvaluations(applicationId, memberId(memberPrincipal))
            .stream()
            .map(RecruitingInterviewEvaluationResponse::from)
            .toList();
    }

    private Long memberId(MemberPrincipal memberPrincipal) {
        return memberPrincipal == null ? null : memberPrincipal.getMemberId();
    }
}
