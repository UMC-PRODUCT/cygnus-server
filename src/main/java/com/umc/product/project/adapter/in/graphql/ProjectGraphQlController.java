package com.umc.product.project.adapter.in.graphql;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.project.adapter.in.graphql.dto.ProjectApplicationFormGraphQlResponse;
import com.umc.product.project.adapter.in.graphql.dto.ProjectApplicationGraphQlResponse;
import com.umc.product.project.adapter.in.graphql.dto.ProjectGraphQlResponse;
import com.umc.product.project.adapter.in.graphql.dto.ProjectMemberGraphQlResponse;
import com.umc.product.project.adapter.in.graphql.dto.ProjectPageGraphQlRequest;
import com.umc.product.project.adapter.in.graphql.dto.ProjectPageGraphQlResponse;
import com.umc.product.project.adapter.in.graphql.dto.ProjectSearchGraphQlRequest;
import com.umc.product.project.application.port.in.query.GetProjectApplicationDetailUseCase;
import com.umc.product.project.application.port.in.query.GetProjectApplicationFormUseCase;
import com.umc.product.project.application.port.in.query.GetProjectMemberUseCase;
import com.umc.product.project.application.port.in.query.GetProjectUseCase;
import com.umc.product.project.application.port.in.query.SearchProjectUseCase;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;
import com.umc.product.project.application.port.in.query.dto.GetProjectApplicationDetailQuery;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationDetailInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectMemberInfo;
import com.umc.product.project.application.port.in.query.dto.SearchProjectQuery;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ProjectGraphQlController {

    private final GetProjectUseCase getProjectUseCase;
    private final SearchProjectUseCase searchProjectUseCase;
    private final GetProjectMemberUseCase getProjectMemberUseCase;
    private final GetProjectApplicationFormUseCase getProjectApplicationFormUseCase;
    private final GetProjectApplicationDetailUseCase getProjectApplicationDetailUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final CheckPermissionUseCase checkPermissionUseCase;

    @QueryMapping
    public ProjectGraphQlResponse project(@Argument Long id) {
        Long requesterMemberId = currentMemberId();
        checkPermissionUseCase.checkOrThrow(requesterMemberId, projectReadPermission(id));
        return ProjectGraphQlResponse.from(getProjectUseCase.getById(id));
    }

    @QueryMapping
    public ProjectPageGraphQlResponse projects(
        @Argument ProjectSearchGraphQlRequest input,
        @Argument ProjectPageGraphQlRequest page
    ) {
        Long requesterMemberId = currentMemberId();
        checkPermissionUseCase.checkOrThrow(
            requesterMemberId,
            ResourcePermission.ofType(ResourceType.PROJECT, PermissionType.READ)
        );

        Pageable pageable = (page == null ? new ProjectPageGraphQlRequest(null, null, null) : page).toPageable();
        SearchProjectQuery query = input.toQuery(pageable);
        return ProjectPageGraphQlResponse.from(searchProjectUseCase.search(query, requesterMemberId));
    }

    @BatchMapping(typeName = "Project", field = "members")
    public Map<ProjectGraphQlResponse, List<ProjectMemberGraphQlResponse>> membersByProject(
        List<ProjectGraphQlResponse> projects
    ) {
        Long requesterMemberId = currentMemberId();
        SubjectAttributes subject = checkPermissionUseCase.loadSubject(requesterMemberId);
        projects.forEach(project -> assertProjectRead(subject, project.id()));

        List<Long> projectIds = uniqueProjectIds(projects);
        Map<Long, List<ProjectMemberInfo>> membersByProjectId = getProjectMemberUseCase.listByProjectIds(projectIds);

        return projects.stream()
            .collect(Collectors.toMap(
                Function.identity(),
                project -> membersByProjectId.getOrDefault(project.id(), List.of()).stream()
                    .map(ProjectMemberGraphQlResponse::from)
                    .toList(),
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    @BatchMapping(typeName = "Project", field = "applicationForm")
    public Map<ProjectGraphQlResponse, ProjectApplicationFormGraphQlResponse> applicationFormByProject(
        List<ProjectGraphQlResponse> projects
    ) {
        Long requesterMemberId = currentMemberId();
        SubjectAttributes subject = checkPermissionUseCase.loadSubject(requesterMemberId);
        projects.forEach(project -> assertProjectRead(subject, project.id()));

        List<Long> projectIds = uniqueProjectIds(projects);
        Map<Long, ApplicationFormInfo> formsByProjectId =
            getProjectApplicationFormUseCase.findAllByProjectIds(projectIds, requesterMemberId);

        Map<ProjectGraphQlResponse, ProjectApplicationFormGraphQlResponse> result = new LinkedHashMap<>();
        for (ProjectGraphQlResponse project : projects) {
            ApplicationFormInfo form = formsByProjectId.get(project.id());
            result.put(project, form == null ? null : ProjectApplicationFormGraphQlResponse.from(form));
        }
        return result;
    }

    @BatchMapping(typeName = "Project", field = "productOwner")
    public Map<ProjectGraphQlResponse, MemberInfo> productOwnerByProject(
        List<ProjectGraphQlResponse> projects
    ) {
        Set<Long> memberIds = projects.stream()
            .map(ProjectGraphQlResponse::productOwnerMemberId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, MemberInfo> membersById = findMembers(memberIds);

        Map<ProjectGraphQlResponse, MemberInfo> result = new LinkedHashMap<>();
        for (ProjectGraphQlResponse project : projects) {
            result.put(project, membersById.get(project.productOwnerMemberId()));
        }
        return result;
    }

    @BatchMapping(typeName = "Project", field = "coProductOwners")
    public Map<ProjectGraphQlResponse, List<MemberInfo>> coProductOwnersByProject(
        List<ProjectGraphQlResponse> projects
    ) {
        Set<Long> memberIds = projects.stream()
            .flatMap(project -> project.coProductOwnerMemberIds() == null
                ? Stream.empty()
                : project.coProductOwnerMemberIds().stream())
            .collect(Collectors.toSet());
        Map<Long, MemberInfo> membersById = findMembers(memberIds);

        return projects.stream()
            .collect(Collectors.toMap(
                Function.identity(),
                project -> project.coProductOwnerMemberIds() == null
                    ? List.of()
                    : project.coProductOwnerMemberIds().stream()
                        .map(membersById::get)
                        .filter(Objects::nonNull)
                        .toList(),
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    @BatchMapping(typeName = "ProjectMember", field = "member")
    public Map<ProjectMemberGraphQlResponse, MemberInfo> memberByProjectMember(
        List<ProjectMemberGraphQlResponse> projectMembers
    ) {
        Set<Long> memberIds = projectMembers.stream()
            .map(ProjectMemberGraphQlResponse::memberId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, MemberInfo> membersById = findMembers(memberIds);

        Map<ProjectMemberGraphQlResponse, MemberInfo> result = new LinkedHashMap<>();
        for (ProjectMemberGraphQlResponse projectMember : projectMembers) {
            result.put(projectMember, membersById.get(projectMember.memberId()));
        }
        return result;
    }

    @BatchMapping(typeName = "ProjectMember", field = "application")
    public Map<ProjectMemberGraphQlResponse, ProjectApplicationGraphQlResponse> applicationByProjectMember(
        List<ProjectMemberGraphQlResponse> projectMembers
    ) {
        Long requesterMemberId = currentMemberId();
        SubjectAttributes subject = checkPermissionUseCase.loadSubject(requesterMemberId);

        Map<Long, GetProjectApplicationDetailQuery> queriesByApplicationId = new LinkedHashMap<>();
        for (ProjectMemberGraphQlResponse projectMember : projectMembers) {
            Long applicationId = projectMember.applicationId();
            if (applicationId == null) {
                continue;
            }
            if (!checkPermissionUseCase.check(subject, applicationReadPermission(applicationId))) {
                continue;
            }
            queriesByApplicationId.putIfAbsent(
                applicationId,
                GetProjectApplicationDetailQuery.builder()
                    .projectId(projectMember.projectId())
                    .applicationId(applicationId)
                    .requesterMemberId(requesterMemberId)
                    .build()
            );
        }

        Map<Long, ProjectApplicationDetailInfo> detailsByApplicationId = queriesByApplicationId.isEmpty()
            ? Map.of()
            : getProjectApplicationDetailUseCase.batchGetDetails(queriesByApplicationId.values());

        Map<ProjectMemberGraphQlResponse, ProjectApplicationGraphQlResponse> result = new LinkedHashMap<>();
        for (ProjectMemberGraphQlResponse projectMember : projectMembers) {
            ProjectApplicationDetailInfo detail = detailsByApplicationId.get(projectMember.applicationId());
            result.put(projectMember, detail == null ? null : ProjectApplicationGraphQlResponse.from(detail));
        }
        return result;
    }

    private Long currentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("로그인이 필요해요. 로그인 후 다시 시도해주세요.");
        }
        if (authentication.getPrincipal() instanceof MemberPrincipal principal) {
            return principal.getMemberId();
        }
        throw new AccessDeniedException("인증 정보가 올바르지 않아요. 다시 로그인해주세요.");
    }

    private void assertProjectRead(SubjectAttributes subject, Long projectId) {
        if (!checkPermissionUseCase.check(subject, projectReadPermission(projectId))) {
            throw new AccessDeniedException("프로젝트를 볼 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요.");
        }
    }

    private ResourcePermission projectReadPermission(Long projectId) {
        return ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ);
    }

    private ResourcePermission applicationReadPermission(Long applicationId) {
        return ResourcePermission.of(ResourceType.PROJECT_APPLICATION, applicationId, PermissionType.READ);
    }

    private List<Long> uniqueProjectIds(List<ProjectGraphQlResponse> projects) {
        return projects.stream()
            .map(ProjectGraphQlResponse::id)
            .collect(Collectors.collectingAndThen(
                Collectors.toCollection(LinkedHashSet::new),
                List::copyOf
            ));
    }

    private Map<Long, MemberInfo> findMembers(Set<Long> memberIds) {
        return memberIds.isEmpty() ? Map.of() : getMemberUseCase.findAllByIds(memberIds);
    }
}
