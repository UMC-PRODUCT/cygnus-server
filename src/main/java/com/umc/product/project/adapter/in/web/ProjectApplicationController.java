package com.umc.product.project.adapter.in.web;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.project.adapter.in.web.dto.request.CreateProjectApplicationRequest;
import com.umc.product.project.adapter.in.web.dto.request.UpdateApplicationAnswersRequest;
import com.umc.product.project.adapter.in.web.dto.request.UpdateApplicationDecisionRequest;
import com.umc.product.project.adapter.in.web.dto.response.ProjectApplicationStatusResponse;
import com.umc.product.project.application.port.in.command.CancelProjectApplicationUseCase;
import com.umc.product.project.application.port.in.command.CreateDraftProjectApplicationUseCase;
import com.umc.product.project.application.port.in.command.DecideApplicationUseCase;
import com.umc.product.project.application.port.in.command.SubmitProjectApplicationUseCase;
import com.umc.product.project.application.port.in.command.UpdateProjectApplicationDraftUseCase;
import com.umc.product.project.application.port.in.command.dto.CancelProjectApplicationCommand;
import com.umc.product.project.application.port.in.command.dto.SubmitProjectApplicationCommand;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Project | 챌린저 지원서", description = "챌린저의 프로젝트 지원서 초안, 임시저장, 제출, 철회를 다룹니다.")
public class ProjectApplicationController {

    private final CreateDraftProjectApplicationUseCase createDraftProjectApplicationUseCase;
    private final UpdateProjectApplicationDraftUseCase updateProjectApplicationDraftUseCase;
    private final SubmitProjectApplicationUseCase submitProjectApplicationUseCase;
    private final DecideApplicationUseCase decideApplicationUseCase;
    private final CancelProjectApplicationUseCase cancelProjectApplicationUseCase;

    @PostMapping("/{projectId}/applications")
    @Operation(
        operationId = "APPLY-001",
        summary = "챌린저 지원서 초안 생성",
        description = "챌린저가 프로젝트 지원서 초안을 생성합니다. 이미 초안이 있으면 기존 지원서 정보를 반환합니다."
    )
    @CheckAccess(
        resourceType = ResourceType.PROJECT_APPLICATION,
        resourceId = "#projectId",
        permission = PermissionType.WRITE,
        message = "지원서를 작성할 권한이 없어요. 지원 가능한 프로젝트인지 확인해주세요."
    )
    public ProjectApplicationStatusResponse createDraft(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long projectId,
        @Valid @RequestBody CreateProjectApplicationRequest request
    ) {
        return ProjectApplicationStatusResponse.from(
            createDraftProjectApplicationUseCase.create(
                request.toCommand(projectId, memberPrincipal.getMemberId())
            )
        );
    }

    @PutMapping("/{projectId}/applications/{applicationId}")
    @Operation(
        operationId = "APPLY-002",
        summary = "챌린저 지원서 임시저장",
        description = "요청 본문을 답변의 새 전체 상태로 저장합니다. 본인의 초안 지원서에서만 호출할 수 있습니다."
    )
    @CheckAccess(
        resourceType = ResourceType.PROJECT_APPLICATION,
        resourceId = "#applicationId",
        permission = PermissionType.EDIT,
        message = "지원서를 임시저장할 권한이 없어요. 지원 가능한 프로젝트인지 확인해주세요."
    )
    public ProjectApplicationStatusResponse updateDraft(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long projectId,
        @PathVariable Long applicationId,
        @Valid @RequestBody UpdateApplicationAnswersRequest request
    ) {
        return ProjectApplicationStatusResponse.from(
            updateProjectApplicationDraftUseCase.update(
                request.toCommand(projectId, applicationId, memberPrincipal.getMemberId())
            )
        );
    }

    @PostMapping("/{projectId}/applications/{applicationId}/submit")
    @Operation(
        operationId = "APPLY-003",
        summary = "챌린저 지원서 최종 제출",
        description = "지원서를 초안에서 제출 상태로 변경합니다. 필수 답변이 빠지면 400을 반환합니다. 본인의 초안 지원서에서만 호출할 수 있습니다."
    )
    @CheckAccess(
        resourceType = ResourceType.PROJECT_APPLICATION,
        resourceId = "#applicationId",
        permission = PermissionType.EDIT,
        message = "지원서를 제출할 권한이 없어요. 지원 가능한 프로젝트인지 확인해주세요."
    )
    public ProjectApplicationStatusResponse submit(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long projectId,
        @PathVariable Long applicationId
    ) {
        return ProjectApplicationStatusResponse.from(
            submitProjectApplicationUseCase.submit(
                SubmitProjectApplicationCommand.builder()
                    .projectId(projectId)
                    .applicationId(applicationId)
                    .requesterMemberId(memberPrincipal.getMemberId())
                    .build()
            )
        );
    }

    @PatchMapping("/{projectId}/applications/{applicationId}/decision")
    @Operation(
        operationId = "APPLY-103",
        summary = "지원서 합격 여부 결정",
        description = """
            PM 또는 SUPER_ADMIN 이 지원서의 status 를 토글합니다.
            - PM: 매칭 차수 진행 중에만 가능 (decisionDeadline 까지)
            - SUPER_ADMIN: 매칭 차수 시간과 관계없이 가능
            - APPROVED ↔ REJECTED 재토글 허용
            - REJECTED 처리 후 매칭 규칙의 최소선발 수를 만족하지 못하면 거절
            """
    )
    @CheckAccess(
        resourceType = ResourceType.PROJECT_APPLICATION,
        resourceId = "#applicationId",
        permission = PermissionType.APPROVE,
        message = "지원서 합격 여부는 권한이 있는 운영진만 결정할 수 있어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    public ProjectApplicationStatusResponse decide(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long projectId,
        @PathVariable Long applicationId,
        @Valid @RequestBody UpdateApplicationDecisionRequest request
    ) {
        return ProjectApplicationStatusResponse.from(
            decideApplicationUseCase.decide(
                applicationId, request.status(), request.reason(), memberPrincipal.getMemberId()
            )
        );
    }

    @DeleteMapping("/{projectId}/applications/{applicationId}")
    @Operation(
        operationId = "APPLY-005",
        summary = "챌린저 지원서 철회",
        description = """
            지원서를 CANCELLED 로 soft delete 합니다.

            정책:
            - 가능 상태: DRAFT, SUBMITTED
            - 불가 상태: APPROVED, REJECTED (이미 종결), CANCELLED (이중 취소)
            - 시간 제약: 지원한 매칭 차수가 OPEN 인 동안만 (startsAt <= now <= endsAt). PM 선발이 시작된 차수 종료 후에는 철회 불가
            - 행위자: 지원자 본인만 (운영진 강제 철회는 별도 API, 추후 작업)

            철회 후 동일 매칭 차수에 재지원 가능 (DB partial unique index 가 활성 지원서 1개 보장).
            Form 응답 본문은 보존됨.
            """
    )
    @CheckAccess(
        resourceType = ResourceType.PROJECT_APPLICATION,
        resourceId = "#applicationId",
        permission = PermissionType.DELETE,
        message = "지원서는 지원자 본인만 철회할 수 있어요."
    )
    public ProjectApplicationStatusResponse cancel(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long applicationId,
        @RequestParam(required = false) String reason
    ) {
        return ProjectApplicationStatusResponse.from(
            cancelProjectApplicationUseCase.cancel(
                CancelProjectApplicationCommand.builder()
                    .applicationId(applicationId)
                    .requesterMemberId(memberPrincipal.getMemberId())
                    .reason(reason)
                    .build()
            )
        );
    }
}
