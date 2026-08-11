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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.global.graphql.relay.ConnectionArguments;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.OffsetPageRequest;
import com.umc.product.global.graphql.relay.RelayConnection;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.project.adapter.in.graphql.dto.MemberBriefGraphQlResponse;
import com.umc.product.project.adapter.in.graphql.dto.ProjectApplicationFormGraphQlResponse;
import com.umc.product.project.adapter.in.graphql.dto.ProjectApplicationGraphQlResponse;
import com.umc.product.project.adapter.in.graphql.dto.ProjectFilterGraphQlRequest;
import com.umc.product.project.adapter.in.graphql.dto.ProjectGraphQlResponse;
import com.umc.product.project.adapter.in.graphql.dto.ProjectMemberGraphQlResponse;
import com.umc.product.project.adapter.in.graphql.dto.ProjectSort;
import com.umc.product.project.application.port.in.query.GetProjectApplicationDetailUseCase;
import com.umc.product.project.application.port.in.query.GetProjectApplicationFormUseCase;
import com.umc.product.project.application.port.in.query.GetProjectMemberUseCase;
import com.umc.product.project.application.port.in.query.GetProjectUseCase;
import com.umc.product.project.application.port.in.query.SearchProjectUseCase;
import com.umc.product.project.application.port.in.query.dto.ApplicationFormInfo;
import com.umc.product.project.application.port.in.query.dto.GetProjectApplicationDetailQuery;
import com.umc.product.project.application.port.in.query.dto.ProjectApplicationDetailInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectInfo;
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
    private final CurrentMemberProvider currentMemberProvider;

    @QueryMapping
    public ProjectGraphQlResponse project(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument String id
    ) {
        Long requesterMemberId = currentMemberId(memberPrincipal);
        Long projectId = GlobalId.decodeLong(id, GlobalIdTypes.PROJECT);
        checkPermissionUseCase.checkOrThrow(requesterMemberId, projectReadPermission(projectId));
        return ProjectGraphQlResponse.from(getProjectUseCase.getById(projectId));
    }

    @QueryMapping
    public RelayConnection<ProjectGraphQlResponse> projects(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument ProjectFilterGraphQlRequest filter,
        @Argument List<ProjectSort> orderBy,
        @Argument Integer first,
        @Argument String after,
        @Argument Integer last,
        @Argument String before
    ) {
        Long requesterMemberId = currentMemberId(memberPrincipal);
        checkPermissionUseCase.checkOrThrow(
            requesterMemberId,
            ResourcePermission.ofType(ResourceType.PROJECT, PermissionType.READ)
        );

        ConnectionArguments arguments = ConnectionArguments.of(first, after, last, before);
        var sort = ProjectSort.toSort(orderBy);
        Pageable window = arguments.toPageable(() -> searchProjectUseCase.search(
            filter.toQuery(new OffsetPageRequest(0, 1, sort)),
            requesterMemberId
        ).getTotalElements());
        Pageable pageable =
            new OffsetPageRequest(window.getOffset(), window.getPageSize(), sort);
        SearchProjectQuery query = filter.toQuery(pageable);
        Page<ProjectInfo> page = searchProjectUseCase.search(query, requesterMemberId);
        return RelayConnection.fromPage(page, arguments, ProjectGraphQlResponse::from);
    }

    @BatchMapping(typeName = "Project", field = "members")
    public Map<ProjectGraphQlResponse, List<ProjectMemberGraphQlResponse>> membersByProject(
        List<ProjectGraphQlResponse> projects
    ) {
        Long requesterMemberId = currentMemberId();
        SubjectAttributes subject = checkPermissionUseCase.loadSubject(requesterMemberId);
        projects.forEach(project -> assertProjectRead(subject, project.projectId()));

        List<Long> projectIds = uniqueProjectIds(projects);
        Map<Long, List<ProjectMemberInfo>> membersByProjectId = getProjectMemberUseCase.listByProjectIds(projectIds);

        return projects.stream()
            .collect(Collectors.toMap(
                Function.identity(),
                project -> membersByProjectId.getOrDefault(project.projectId(), List.of()).stream()
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
        projects.forEach(project -> assertProjectRead(subject, project.projectId()));

        List<Long> projectIds = uniqueProjectIds(projects);
        Map<Long, ApplicationFormInfo> formsByProjectId =
            getProjectApplicationFormUseCase.findAllByProjectIds(projectIds, requesterMemberId);

        Map<ProjectGraphQlResponse, ProjectApplicationFormGraphQlResponse> result = new LinkedHashMap<>();
        for (ProjectGraphQlResponse project : projects) {
            ApplicationFormInfo form = formsByProjectId.get(project.projectId());
            result.put(project, form == null ? null : ProjectApplicationFormGraphQlResponse.from(form));
        }
        return result;
    }

    @BatchMapping(typeName = "Project", field = "productOwner")
    public Map<ProjectGraphQlResponse, MemberBriefGraphQlResponse> productOwnerByProject(
        List<ProjectGraphQlResponse> projects
    ) {
        Set<Long> memberIds = projects.stream()
            .map(ProjectGraphQlResponse::productOwnerMemberId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, MemberInfo> membersById = findMembers(memberIds);

        Map<ProjectGraphQlResponse, MemberBriefGraphQlResponse> result = new LinkedHashMap<>();
        for (ProjectGraphQlResponse project : projects) {
            result.put(project, memberBrief(membersById, project.productOwnerMemberId()));
        }
        return result;
    }

    @BatchMapping(typeName = "Project", field = "coProductOwners")
    public Map<ProjectGraphQlResponse, List<MemberBriefGraphQlResponse>> coProductOwnersByProject(
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
                        .map(memberId -> memberBrief(membersById, memberId))
                        .filter(Objects::nonNull)
                        .toList(),
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    @BatchMapping(typeName = "ProjectMember", field = "member")
    public Map<ProjectMemberGraphQlResponse, MemberBriefGraphQlResponse> memberByProjectMember(
        List<ProjectMemberGraphQlResponse> projectMembers
    ) {
        Set<Long> memberIds = projectMembers.stream()
            .map(ProjectMemberGraphQlResponse::memberId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, MemberInfo> membersById = findMembers(memberIds);

        Map<ProjectMemberGraphQlResponse, MemberBriefGraphQlResponse> result = new LinkedHashMap<>();
        for (ProjectMemberGraphQlResponse projectMember : projectMembers) {
            result.put(projectMember, memberBrief(membersById, projectMember.memberId()));
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

    private Long currentMemberId(MemberPrincipal memberPrincipal) {
        return memberPrincipal == null
            ? currentMemberProvider.getRequiredCurrentMemberId()
            : memberPrincipal.getMemberId();
    }

    private Long currentMemberId() {
        return currentMemberProvider.getRequiredCurrentMemberId();
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
            .map(ProjectGraphQlResponse::projectId)
            .collect(Collectors.collectingAndThen(
                Collectors.toCollection(LinkedHashSet::new),
                List::copyOf
            ));
    }

    private MemberBriefGraphQlResponse memberBrief(Map<Long, MemberInfo> membersById, Long memberId) {
        MemberInfo memberInfo = membersById.get(memberId);
        return memberInfo == null ? null : MemberBriefGraphQlResponse.from(memberInfo);
    }

    private Map<Long, MemberInfo> findMembers(Set<Long> memberIds) {
        return memberIds.isEmpty() ? Map.of() : getMemberUseCase.findAllByIds(memberIds);
    }
}
