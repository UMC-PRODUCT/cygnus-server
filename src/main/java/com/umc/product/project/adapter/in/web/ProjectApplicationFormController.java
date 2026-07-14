package com.umc.product.project.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.project.adapter.in.web.dto.request.UpsertApplicationFormRequest;
import com.umc.product.project.adapter.in.web.dto.response.GetApplicationFormResponse;
import com.umc.product.project.adapter.in.web.dto.response.UpsertApplicationFormResponse;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationSurface;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationSurfaceBinding;
import com.umc.product.project.application.port.in.command.UpsertProjectApplicationFormUseCase;
import com.umc.product.project.application.port.in.query.GetProjectApplicationFormUseCase;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Project | 프로젝트 지원 폼", description = "프로젝트 지원 폼을 저장하고 조회합니다.")
public class ProjectApplicationFormController {

    private final UpsertProjectApplicationFormUseCase upsertProjectApplicationFormUseCase;
    private final GetProjectApplicationFormUseCase getProjectApplicationFormUseCase;

    @PutMapping("/{projectId}/application-form")
    @ProjectAuthorizationSurfaceBinding(ProjectAuthorizationSurface.REST_FORM_UPDATE)
    @Operation(
        operationId = "PROJECT-106",
        summary = "지원 폼 저장",
        description = "요청 본문을 지원 폼의 새 전체 상태로 저장합니다. 폼이 없으면 생성하고, 있으면 섹션, 질문, 옵션을 본문 구조와 맞춥니다. DRAFT 또는 PENDING_REVIEW 상태에서만 호출할 수 있습니다."
    )
    @CheckAccess(
        resourceType = ResourceType.PROJECT,
        resourceId = "#projectId",
        permission = PermissionType.EDIT,
        action = "project-form:update",
        message = "지원 폼을 저장할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    public UpsertApplicationFormResponse upsert(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long projectId,
        @Valid @RequestBody UpsertApplicationFormRequest request
    ) {
        ApplicationFormInfo info = upsertProjectApplicationFormUseCase.upsert(
            request.toCommand(projectId, memberPrincipal.getMemberId()));
        return UpsertApplicationFormResponse.from(info);
    }

    @GetMapping("/{projectId}/application-form")
    @ProjectAuthorizationSurfaceBinding(ProjectAuthorizationSurface.REST_FORM_READ)
    @Operation(
        operationId = "PROJECT-106-GET",
        summary = "지원 폼 조회",
        description = """
            프로젝트의 지원 폼 구조를 조회한다.
            호출자가 PM/총괄단/프로젝트 지부의 지부장이면 전체 섹션, 일반 챌린저이면 본인 파트가 매칭된 PART 섹션과 COMMON 섹션만 노출된다.
            프로젝트 기수에 챌린저 레코드가 없는 외부 사용자는 403. 폼이 없으면 ApiResponse.result = null.
            """
    )
    @CheckAccess(
        resourceType = ResourceType.PROJECT,
        resourceId = "#projectId",
        permission = PermissionType.READ,
        message = "지원 폼을 볼 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    public GetApplicationFormResponse get(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long projectId
    ) {
        return getProjectApplicationFormUseCase
            .findByProjectId(projectId, memberPrincipal.getMemberId())
            .map(GetApplicationFormResponse::from)
            .orElse(null);
    }
}
