package com.umc.product.project.adapter.in.web;

import java.util.List;
import java.util.Map;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.response.PageResponse;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.project.adapter.in.web.assembler.ProjectResponseAssembler;
import com.umc.product.project.adapter.in.web.dto.request.SearchProjectRequest;
import com.umc.product.project.adapter.in.web.dto.response.DraftProjectResponse;
import com.umc.product.project.adapter.in.web.dto.response.ManagedProjectSummaryResponse;
import com.umc.product.project.adapter.in.web.dto.response.ProjectDetailResponse;
import com.umc.product.project.adapter.in.web.dto.response.ProjectMembersResponse;
import com.umc.product.project.adapter.in.web.dto.response.ProjectSummaryResponse;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationSurface;
import com.umc.product.project.application.authorization.rollout.ProjectAuthorizationSurfaceBinding;
import com.umc.product.project.application.port.in.query.dto.SearchManagedProjectQuery;
import com.umc.product.project.application.port.in.query.dto.SearchProjectQuery;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Project | 프로젝트 Query", description = "프로젝트와 관련 정보를 조회합니다.")
public class ProjectQueryController {

    private final ProjectResponseAssembler assembler;

    @GetMapping
    @ProjectAuthorizationSurfaceBinding(ProjectAuthorizationSurface.REST_PROJECT_LIST_PUBLIC)
    @Operation(
        operationId = "PROJECT-001",
        summary = "프로젝트 목록 조회",
        description = "기수/지부/파트 등으로 필터링된 프로젝트 목록을 페이지 조회합니다."
    )
    @CheckAccess(
        resourceType = ResourceType.PROJECT,
        permission = PermissionType.READ,
        message = "프로젝트를 볼 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    public PageResponse<ProjectSummaryResponse> searchProjects(
        @CurrentMember MemberPrincipal memberPrincipal,
        @ParameterObject @Valid SearchProjectRequest request,
        @ParameterObject @PageableDefault(size = 20, sort = {"createdAt", "name"}, direction = Sort.Direction.ASC)
        Pageable pageable
    ) {
        SearchProjectQuery query = request.toQuery(pageable);
        return assembler.searchFor(query, memberPrincipal.getMemberId());
    }

    @GetMapping("/{projectId}")
    @ProjectAuthorizationSurfaceBinding(ProjectAuthorizationSurface.REST_PROJECT_READ)
    @Operation(
        operationId = "PROJECT-002",
        summary = "프로젝트 상세 조회",
        description = "단건 프로젝트 상세 정보를 조회합니다. 권한에 따라 실명 정보가 마스킹됩니다."
    )
    @CheckAccess(
        resourceType = ResourceType.PROJECT,
        resourceId = "#projectId",
        permission = PermissionType.READ,
        action = "project:read",
        message = "프로젝트를 볼 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    public ProjectDetailResponse getDetail(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long projectId
    ) {
        return assembler.detailFor(projectId);
    }

    @GetMapping("/{projectId}/members")
    @ProjectAuthorizationSurfaceBinding(ProjectAuthorizationSurface.REST_PROJECT_MEMBER_LIST)
    @Operation(
        operationId = "PROJECT-003",
        summary = "프로젝트 팀원 구성 조회",
        description = "프로젝트의 PM/보조 PM/파트별 멤버를 조회합니다. 권한에 따라 실명이 마스킹됩니다."
    )
    @CheckAccess(
        resourceType = ResourceType.PROJECT,
        resourceId = "#projectId",
        permission = PermissionType.READ,
        action = "project-member:list",
        message = "프로젝트를 볼 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    public ProjectMembersResponse getMembers(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long projectId
    ) {
        return assembler.membersFor(projectId);
    }

    @GetMapping("/members")
    @ProjectAuthorizationSurfaceBinding(ProjectAuthorizationSurface.REST_PROJECT_MEMBER_BATCH)
    @Operation(
        operationId = "PROJECT-007",
        summary = "프로젝트 팀원 구성 일괄 조회",
        description = "복수의 projectId에 대한 팀원 구성을 조회합니다. 권한이 없거나 조회에 실패한 프로젝트는 결과에서 제외됩니다."
    )
    @CheckAccess(
        resourceType = ResourceType.PROJECT,
        permission = PermissionType.READ,
        action = "project-member:batch",
        message = "프로젝트를 볼 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    public Map<Long, ProjectMembersResponse> getBatchMembers(
        @CurrentMember MemberPrincipal memberPrincipal,
        @RequestParam List<Long> projectIds
    ) {
        return assembler.listProjectMembers(projectIds, memberPrincipal.getMemberId());
    }

    @GetMapping("/me/managed")
    @ProjectAuthorizationSurfaceBinding(ProjectAuthorizationSurface.REST_PROJECT_LIST_MANAGED)
    @Operation(
        operationId = "PROJECT-006",
        summary = "내가 관리하는 프로젝트 목록",
        description = "역할별 자동 scope: 중앙 총괄단은 전체, 지부장과 학교 회장단은 본인이 속한 지부 전체, PM 챌린저는 본인 owner 프로젝트. 일반 챌린저는 빈 페이지."
    )
    public PageResponse<ManagedProjectSummaryResponse> searchManaged(
        @CurrentMember MemberPrincipal memberPrincipal,
        @RequestParam Long gisuId,
        @RequestParam(required = false) String keyword,
        @ParameterObject @PageableDefault(size = 20, sort = {"createdAt", "name"}, direction = Sort.Direction.ASC)
        Pageable pageable
    ) {
        SearchManagedProjectQuery query = SearchManagedProjectQuery.builder()
            .gisuId(gisuId)
            .keyword(keyword)
            .pageable(pageable)
            .build();
        return assembler.searchManagedFor(query, memberPrincipal.getMemberId());
    }

    @GetMapping("/me/draft")
    @ProjectAuthorizationSurfaceBinding(ProjectAuthorizationSurface.REST_PROJECT_LIST_OWN_DRAFTS)
    @Operation(
        operationId = "PROJECT-103",
        summary = "내 초안 프로젝트 조회",
        description = "요청자(PM)가 작성 중인 Draft 프로젝트를 조회합니다. 없으면 null."
    )
    public DraftProjectResponse getMyDraft(
        @CurrentMember MemberPrincipal memberPrincipal,
        @RequestParam Long gisuId
    ) {
        return assembler.draftFor(memberPrincipal.getMemberId(), gisuId);
    }
}
