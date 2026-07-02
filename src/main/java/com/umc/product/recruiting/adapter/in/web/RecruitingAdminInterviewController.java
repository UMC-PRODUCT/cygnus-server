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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recruiting/admin")
@Tag(name = "Recruiting | 면접 관리", description = "운영진이 면접 배정, 일정 안내, 면접 평가를 관리합니다.")
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
    @Operation(
        operationId = "RECRUITING-INTERVIEW-001",
        summary = "면접 배정",
        description = "서류 합격 지원서에 면접관과 면접 일정을 배정합니다."
    )
    public RecruitingIdResponse assignInterview(
        @Parameter(hidden = true)
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
    @Operation(
        operationId = "RECRUITING-INTERVIEW-002",
        summary = "면접 스킵",
        description = "면접을 진행하지 않는 지원서의 면접 단계를 스킵합니다."
    )
    public void skipInterview(
        @Parameter(hidden = true)
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
    @Operation(
        operationId = "RECRUITING-INTERVIEW-003",
        summary = "면접 일정 후보 조회",
        description = "form 응답의 가능 일정 데이터를 기반으로 겹치는 면접 일정 후보를 조회합니다."
    )
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
    @Operation(
        operationId = "RECRUITING-INTERVIEW-004",
        summary = "면접 안내 메일 발송",
        description = "지원자에게 면접 시작 시간과 장소 안내 메일을 발송합니다."
    )
    public void sendInterviewGuide(
        @PathVariable Long seasonId,
        @PathVariable Long applicationId,
        @Valid @RequestBody SendRecruitingInterviewGuideRequest request
    ) {
        sendInterviewGuideUseCase.sendGuide(request.toCommand(applicationId));
    }

    @PatchMapping("/seasons/{seasonId}/interview-assignments/{assignmentId}/evaluation")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    @Operation(
        operationId = "RECRUITING-INTERVIEW-005",
        summary = "면접 평가 임시 저장",
        description = "면접관이 지원자 평가 점수와 코멘트를 임시 저장합니다."
    )
    public void saveEvaluation(
        @Parameter(hidden = true)
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long seasonId,
        @PathVariable Long assignmentId,
        @Valid @RequestBody SaveRecruitingInterviewEvaluationRequest request
    ) {
        saveEvaluationUseCase.saveEvaluation(request.toSaveCommand(assignmentId, memberId(memberPrincipal)));
    }

    @PostMapping("/seasons/{seasonId}/interview-assignments/{assignmentId}/evaluation/submit")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    @Operation(
        operationId = "RECRUITING-INTERVIEW-006",
        summary = "면접 평가 제출",
        description = "면접관이 지원자 평가를 제출합니다. 제출 후에는 visibility 정책에 따라 다른 면접관의 평가를 볼 수 있습니다."
    )
    public void submitEvaluation(
        @Parameter(hidden = true)
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long seasonId,
        @PathVariable Long assignmentId,
        @Valid @RequestBody SaveRecruitingInterviewEvaluationRequest request
    ) {
        submitEvaluationUseCase.submitEvaluation(request.toSubmitCommand(assignmentId, memberId(memberPrincipal)));
    }

    @GetMapping("/seasons/{seasonId}/applications/{applicationId}/interview-evaluations")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.READ)
    @Operation(
        operationId = "RECRUITING-INTERVIEW-007",
        summary = "면접 평가 목록 조회",
        description = "관리자가 특정 지원서에 작성된 면접 평가 목록을 조회합니다."
    )
    public List<RecruitingInterviewEvaluationResponse> listVisibleEvaluations(
        @Parameter(hidden = true)
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long seasonId,
        @PathVariable Long applicationId
    ) {
        return getEvaluationUseCase.listVisibleEvaluations(applicationId, memberId(memberPrincipal), true)
            .stream()
            .map(RecruitingInterviewEvaluationResponse::from)
            .toList();
    }

    private Long memberId(MemberPrincipal memberPrincipal) {
        return memberPrincipal == null ? null : memberPrincipal.getMemberId();
    }
}
