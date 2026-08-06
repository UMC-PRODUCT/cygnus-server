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
import com.umc.product.project.adapter.in.web.dto.request.AbortProjectRequest;
import com.umc.product.project.adapter.in.web.dto.request.AddProjectMemberRequest;
import com.umc.product.project.adapter.in.web.dto.request.ChangeProjectMemberStatusRequest;
import com.umc.product.project.adapter.in.web.dto.request.CreateDraftProjectRequest;
import com.umc.product.project.adapter.in.web.dto.request.TransferProjectOwnershipRequest;
import com.umc.product.project.adapter.in.web.dto.request.UpdatePartQuotasRequest;
import com.umc.product.project.adapter.in.web.dto.request.UpdateProjectRequest;
import com.umc.product.project.adapter.in.web.dto.response.ProjectStatusResponse;
import com.umc.product.project.application.port.in.command.AbortProjectUseCase;
import com.umc.product.project.application.port.in.command.AddProjectMemberUseCase;
import com.umc.product.project.application.port.in.command.ChangeProjectMemberStatusUseCase;
import com.umc.product.project.application.port.in.command.CreateDraftProjectUseCase;
import com.umc.product.project.application.port.in.command.DeleteProjectUseCase;
import com.umc.product.project.application.port.in.command.PublishProjectUseCase;
import com.umc.product.project.application.port.in.command.RemoveProjectMemberUseCase;
import com.umc.product.project.application.port.in.command.SubmitProjectUseCase;
import com.umc.product.project.application.port.in.command.TransferProjectOwnershipUseCase;
import com.umc.product.project.application.port.in.command.UpdatePartQuotasUseCase;
import com.umc.product.project.application.port.in.command.UpdateProjectUseCase;
import com.umc.product.project.application.port.in.command.dto.DeleteProjectCommand;
import com.umc.product.project.application.port.in.command.dto.PublishProjectCommand;
import com.umc.product.project.application.port.in.command.dto.RemoveProjectMemberCommand;
import com.umc.product.project.application.port.in.command.dto.SubmitProjectCommand;
import com.umc.product.project.domain.enums.ProjectStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Project | 프로젝트 Command", description = "프로젝트와 참여자를 만들고 관리합니다.")
public class ProjectCommandController {

    private final CreateDraftProjectUseCase createDraftProjectUseCase;
    private final UpdateProjectUseCase updateProjectUseCase;
    private final SubmitProjectUseCase submitProjectUseCase;
    private final TransferProjectOwnershipUseCase transferProjectOwnershipUseCase;
    private final AddProjectMemberUseCase addProjectMemberUseCase;
    private final RemoveProjectMemberUseCase removeProjectMemberUseCase;
    private final ChangeProjectMemberStatusUseCase changeProjectMemberStatusUseCase;
    private final UpdatePartQuotasUseCase updatePartQuotasUseCase;
    private final PublishProjectUseCase publishProjectUseCase;
    private final DeleteProjectUseCase deleteProjectUseCase;
    private final AbortProjectUseCase abortProjectUseCase;

    @PostMapping
    @Operation(
        operationId = "PROJECT-101",
        summary = "프로젝트 초안 생성",
        description = "PM(PLAN 파트 챌린저)이 빈 DRAFT 상태의 프로젝트를 생성합니다. 페이지 진입 시 GET /me/draft로 사전 확인 후 호출 권장. 동일 PM·동일 기수 중복 생성 시 409."
    )
    @CheckAccess(
        resourceType = ResourceType.PROJECT,
        permission = PermissionType.WRITE,
        message = "프로젝트를 만들 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    public ProjectStatusResponse createDraft(
        @CurrentMember MemberPrincipal memberPrincipal,
        @Valid @RequestBody CreateDraftProjectRequest request
    ) {
        Long projectId = createDraftProjectUseCase.create(
            request.toCommand(memberPrincipal.getMemberId()));
        return ProjectStatusResponse.of(projectId, ProjectStatus.DRAFT);
    }

    @PatchMapping("/{projectId}")
    @Operation(
        operationId = "PROJECT-102",
        summary = "프로젝트 기본정보 수정",
        description = "프로젝트 기본정보를 부분 업데이트합니다. DRAFT/PENDING_REVIEW/IN_PROGRESS 모두 허용, 종료 상태(COMPLETED/ABORTED)는 수정 불가. 소유권 양도는 별도 엔드포인트."
    )
    @CheckAccess(
        resourceType = ResourceType.PROJECT,
        resourceId = "#projectId",
        permission = PermissionType.EDIT,
        message = "프로젝트를 수정할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    public ProjectStatusResponse update(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long projectId,
        @Valid @RequestBody UpdateProjectRequest request
    ) {
        ProjectStatus status = updateProjectUseCase.update(
            request.toCommand(projectId, memberPrincipal.getMemberId()));
        return ProjectStatusResponse.of(projectId, status);
    }

    @PostMapping("/{projectId}/submit")
    @Operation(
        operationId = "PROJECT-107",
        summary = "프로젝트 제출",
        description = "DRAFT 상태의 프로젝트를 제출하여 PENDING_REVIEW로 전이합니다. 작성자(creator)만 호출 가능."
    )
    @CheckAccess(
        resourceType = ResourceType.PROJECT,
        resourceId = "#projectId",
        permission = PermissionType.EDIT,
        message = "프로젝트를 제출할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    public ProjectStatusResponse submit(
        @PathVariable Long projectId
    ) {
        submitProjectUseCase.submit(SubmitProjectCommand.builder()
            .projectId(projectId)
            .build());
        return ProjectStatusResponse.of(projectId, ProjectStatus.PENDING_REVIEW);
    }

    @PostMapping("/{projectId}/transfer-ownership")
    @Operation(
        operationId = "PROJECT-104",
        summary = "프로젝트 소유권 양도",
        description = "메인 PM을 다른 PLAN 파트 챌린저에게 양도합니다. 현재 PM 또는 운영진이 호출 가능. 종료 상태에서는 호출 불가."
    )
    @CheckAccess(
        resourceType = ResourceType.PROJECT,
        resourceId = "#projectId",
        permission = PermissionType.EDIT,
        message = "프로젝트 소유권을 양도할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    public ProjectStatusResponse transferOwnership(
        @PathVariable Long projectId,
        @Valid @RequestBody TransferProjectOwnershipRequest request
    ) {
        ProjectStatus status = transferProjectOwnershipUseCase.transfer(
            request.toCommand(projectId));
        return ProjectStatusResponse.of(projectId, status);
    }

    @PostMapping("/{projectId}/members")
    @Operation(
        operationId = "PROJECT-004",
        summary = "프로젝트 팀원 추가",
        description = "프로젝트에 멤버를 추가합니다. 보조 PM 추가는 part = PLAN. DRAFT 단계에선 PM 본인만, IN_PROGRESS 에선 운영진(중앙 총괄단)도 호출 가능."
    )
    @CheckAccess(
        resourceType = ResourceType.PROJECT,
        resourceId = "#projectId",
        permission = PermissionType.EDIT,
        message = "프로젝트 팀원을 추가할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    public Long addMember(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long projectId,
        @Valid @RequestBody AddProjectMemberRequest request
    ) {
        return addProjectMemberUseCase.add(
            request.toCommand(projectId, memberPrincipal.getMemberId()));
    }

    @PostMapping("/{projectId}/publish")
    @Operation(
        operationId = "PROJECT-108",
        summary = "프로젝트 공개",
        description = "PENDING_REVIEW → IN_PROGRESS 전이. 같은 트랜잭션에서 지원 폼도 PUBLISHED 로 전환. 파트별 정원 1개 이상 + 지원 폼 등록 필수. 운영진(본인 지부장 또는 Central Core)만 호출 가능 — PM 도 차단."
    )
    @CheckAccess(
        resourceType = ResourceType.PROJECT,
        resourceId = "#projectId",
        permission = PermissionType.MANAGE,
        message = "프로젝트를 공개할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    public ProjectStatusResponse publish(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long projectId
    ) {
        ProjectStatus status = publishProjectUseCase.publish(PublishProjectCommand.builder()
            .projectId(projectId)
            .requesterMemberId(memberPrincipal.getMemberId())
            .build());
        return ProjectStatusResponse.of(projectId, status);
    }

    @PutMapping("/{projectId}/part-quotas")
    @Operation(
        operationId = "PROJECT-105",
        summary = "파트별 정원 일괄 갱신",
        description = "PUT 시멘틱 — 본문이 곧 새 상태가 된다. 본문에 없는 기존 파트는 삭제. quota ≥ 1. 운영진 검토 단계 액션 — 본인 지부장 또는 총괄단만 호출 가능."
    )
    @CheckAccess(
        resourceType = ResourceType.PROJECT,
        resourceId = "#projectId",
        permission = PermissionType.MANAGE,
        message = "프로젝트 파트 정원을 수정할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    public void updatePartQuotas(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long projectId,
        @Valid @RequestBody UpdatePartQuotasRequest request
    ) {
        updatePartQuotasUseCase.update(
            request.toCommand(projectId, memberPrincipal.getMemberId()));
    }

    @DeleteMapping("/{projectId}")
    @Operation(
        operationId = "PROJECT-109",
        summary = "프로젝트 삭제",
        description = "DRAFT / PENDING_REVIEW 상태의 프로젝트를 hard delete 합니다. 연관 ProjectMember / PartQuota / ApplicationForm + form Form 까지 cascade 삭제. PO 본인 또는 운영진(본인 지부장 / 해당 기수 총괄단)만 호출 가능. IN_PROGRESS 이상은 abort 엔드포인트 사용."
    )
    @CheckAccess(
        resourceType = ResourceType.PROJECT,
        resourceId = "#projectId",
        permission = PermissionType.DELETE,
        message = "프로젝트를 삭제할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    public void delete(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long projectId
    ) {
        deleteProjectUseCase.delete(DeleteProjectCommand.builder()
            .projectId(projectId)
            .requesterMemberId(memberPrincipal.getMemberId())
            .build());
    }

    @PostMapping("/{projectId}/abort")
    @Operation(
        operationId = "PROJECT-110",
        summary = "프로젝트 중단",
        description = "IN_PROGRESS 상태의 프로젝트를 ABORTED 로 전이합니다. ACTIVE ProjectMember 는 WITHDRAWN, 진행 중(DRAFT/SUBMITTED) ProjectApplication 은 CANCELLED 로 일괄 동기화. 운영진(본인 지부장 또는 Central Core) 만 호출 가능."
    )
    @CheckAccess(
        resourceType = ResourceType.PROJECT,
        resourceId = "#projectId",
        permission = PermissionType.MANAGE,
        message = "프로젝트를 중단할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    public void abort(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long projectId,
        @Valid @RequestBody AbortProjectRequest request
    ) {
        abortProjectUseCase.abort(request.toCommand(projectId, memberPrincipal.getMemberId()));
    }

    @DeleteMapping("/{projectId}/members/{memberId}")
    @Operation(
        operationId = "PROJECT-005",
        summary = "프로젝트 팀원 제거 (hard delete)",
        description = """
            프로젝트에서 멤버 행을 완전히 삭제합니다. 동일 멤버를 같은 프로젝트에 재등록할 수 있습니다.
            DRAFT/PENDING_REVIEW/IN_PROGRESS 프로젝트 단계에서만 가능하며, 종료(COMPLETED/ABORTED) 프로젝트는 이력 보존을 위해 거부됩니다.
            상태 변경 히스토리를 남겨야 하면 [PROJECT-006] API 를 사용하세요.
            메인 PM 은 양도 API 로 변경해야 합니다.
            reason 은 DB 에는 남지 않고 감사 로그로만 기록됩니다.
            """
    )
    @CheckAccess(
        resourceType = ResourceType.PROJECT,
        resourceId = "#projectId",
        permission = PermissionType.EDIT,
        message = "프로젝트 팀원을 제거할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    public void removeMember(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long projectId,
        @PathVariable Long memberId,
        @RequestParam(required = false) String reason
    ) {
        removeProjectMemberUseCase.remove(RemoveProjectMemberCommand.builder()
            .projectId(projectId)
            .memberId(memberId)
            .reason(reason)
            .requesterMemberId(memberPrincipal.getMemberId())
            .build());
    }

    @PatchMapping("/{projectId}/members/{memberId}/status")
    @Operation(
        operationId = "PROJECT-006",
        summary = "프로젝트 팀원 상태 변경 (soft delete)",
        description = """
            멤버 행을 보존한 채 status 를 변경합니다(COMPLETED/WITHDRAWN/DISMISSED 등).
            변경 사유(reason)는 필수입니다.
            종료(COMPLETED/ABORTED) 프로젝트와 메인 PM 은 거부됩니다. 동일 멤버 재등록이 필요하면 hard delete(DELETE) 를 사용하세요.
            """
    )
    @CheckAccess(
        resourceType = ResourceType.PROJECT,
        resourceId = "#projectId",
        permission = PermissionType.EDIT,
        message = "프로젝트 팀원 상태를 변경할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    public void changeMemberStatus(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long projectId,
        @PathVariable Long memberId,
        @Valid @RequestBody ChangeProjectMemberStatusRequest request
    ) {
        changeProjectMemberStatusUseCase.changeStatus(
            request.toCommand(projectId, memberId, memberPrincipal.getMemberId()));
    }
}
