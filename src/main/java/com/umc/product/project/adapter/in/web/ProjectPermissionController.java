package com.umc.product.project.adapter.in.web;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.project.adapter.in.web.dto.response.ProjectPermissionsResponse;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationSurface;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationSurfaceBinding;
import com.umc.product.project.application.port.in.query.GetProjectPermissionsUseCase;
import com.umc.product.project.application.port.in.query.dto.ProjectPermissionInfo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Project | 프로젝트 권한", description = "프로젝트 화면 분기용 capability를 조회합니다.")
public class ProjectPermissionController {

    private final GetProjectPermissionsUseCase getProjectPermissionsUseCase;

    @GetMapping("/permissions")
    @ProjectAuthorizationSurfaceBinding(ProjectAuthorizationSurface.REST_CAPABILITY_LIST)
    @Operation(
        operationId = "PROJECT-PERMISSIONS-001",
        summary = "프로젝트 capability 일괄 조회",
        description = "프로젝트 단위 화면 분기용 권한과 상태 기반 capability를 조회합니다. 권한이 없어도 false capability를 반환합니다."
    )
    public ProjectPermissionsResponse getPermissions(
        @CurrentMember MemberPrincipal memberPrincipal,
        @RequestParam(name = "ids")
        @NotEmpty @Size(max = 100, message = "ids는 최대 100개까지 조회할 수 있습니다.") List<@NotNull Long> ids
    ) {
        List<Long> deduplicatedIds = new ArrayList<>(new LinkedHashSet<>(ids));
        List<ProjectPermissionInfo> infos = getProjectPermissionsUseCase.listByProjectIds(
            memberPrincipal.getMemberId(),
            deduplicatedIds
        );
        return ProjectPermissionsResponse.from(infos);
    }
}
