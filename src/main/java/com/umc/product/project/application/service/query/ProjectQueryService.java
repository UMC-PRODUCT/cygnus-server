package com.umc.product.project.application.service.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.application.access.ProjectAccessScope;
import com.umc.product.project.application.access.ProjectAccessScope.All;
import com.umc.product.project.application.access.ProjectAccessScope.ChapterScoped;
import com.umc.product.project.application.access.ProjectAccessScope.None;
import com.umc.product.project.application.access.ProjectAccessScope.OwnerOnly;
import com.umc.product.project.application.access.ProjectAccessScope.PublicOnly;
import com.umc.product.project.application.access.ProjectAccessScopeResolver;
import com.umc.product.project.application.port.in.query.GetProjectUseCase;
import com.umc.product.project.application.port.in.query.SearchManagedProjectUseCase;
import com.umc.product.project.application.port.in.query.SearchProjectUseCase;
import com.umc.product.project.application.port.in.query.dto.ProjectInfo;
import com.umc.product.project.application.port.in.query.dto.ProjectPartQuotaInfo;
import com.umc.product.project.application.port.in.query.dto.SearchManagedProjectQuery;
import com.umc.product.project.application.port.in.query.dto.SearchProjectQuery;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.application.port.out.LoadProjectPartQuotaPort;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.ProjectMember;
import com.umc.product.project.domain.ProjectPartQuota;
import com.umc.product.project.domain.enums.ProjectStatus;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectQueryService implements
    GetProjectUseCase,
    SearchProjectUseCase,
    SearchManagedProjectUseCase {

    private final LoadProjectPort loadProjectPort;
    private final LoadProjectMemberPort loadProjectMemberPort;
    private final LoadProjectPartQuotaPort loadProjectPartQuotaPort;
    private final ProjectAccessScopeResolver scopeResolver;

    // Cross-domain
    private final GetFileUseCase getFileUseCase;

    @Override
    public ProjectInfo getById(Long projectId) {
        Project project = loadProjectPort.getById(projectId);
        return toProjectInfo(project);
    }

    /**
     * Assembler 가 여러 projectId 의 ProjectInfo 를 N+1 없이 한 번에 받기 위한 batch 진입점.
     * <p>
     * coPM 멤버 / 파트별 TO / 현재 활성 멤버 수 / 썸네일/로고 CDN URL 합성에 들어가는 모든 추가 조회를 IN 쿼리 1 회로 묶어 처리한다. 누락된 projectId 는 결과 Map 에서
     * 빠진다.
     */
    @Override
    public Map<Long, ProjectInfo> findAllByIds(Collection<Long> projectIds) {
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        List<Project> projects = loadProjectPort.listByIds(projectIds);
        if (projects.isEmpty()) {
            return Map.of();
        }

        Set<Long> ids = projects.stream().map(Project::getId).collect(Collectors.toSet());

        Map<Long, List<ProjectMember>> planMembersByProject =
            loadProjectMemberPort.listByProjectIdsAndPartGroupedByProjectId(ids, ChallengerPart.PLAN);
        Map<Long, List<ProjectPartQuota>> quotasByProject =
            loadProjectPartQuotaPort.listByProjectIdsGroupedByProjectId(ids);
        Map<Long, Map<ChallengerPart, Long>> memberCountsByProject =
            loadProjectMemberPort.countByProjectIdsGroupByProjectIdAndPart(ids);
        Map<String, String> fileLinks = resolveFileLinks(projects);

        Map<Long, ProjectInfo> result = new LinkedHashMap<>();
        for (Project project : projects) {
            result.put(project.getId(), assembleProjectInfo(
                project,
                planMembersByProject.getOrDefault(project.getId(), List.of()),
                quotasByProject.getOrDefault(project.getId(), List.of()),
                memberCountsByProject.getOrDefault(project.getId(), Map.of()),
                fileLinks
            ));
        }
        return result;
    }

    @Override
    public Optional<ProjectInfo> findDraftByCreatorAndGisu(Long creatorMemberId, Long gisuId) {
        ProjectAccessScope scope = scopeResolver.resolveForOwnDraft(creatorMemberId, gisuId);
        if (!(scope instanceof OwnerOnly ownerOnly)
            || !Objects.equals(ownerOnly.memberId(), creatorMemberId)
            || !ownerOnly.visibleStatuses().contains(ProjectStatus.DRAFT)) {
            return Optional.empty();
        }
        return loadProjectPort.findDraftByCreatorAndGisu(creatorMemberId, gisuId)
            .map(this::toProjectInfo);
    }

    /**
     * 관리 화면 검색 (PROJECT-006). 호출자 역할에 따라 자동 scope 적용.
     * <p>
     * 노출 상태: PENDING_REVIEW / IN_PROGRESS / COMPLETED / ABORTED. DRAFT 는 제외.
     */
    @Override
    public Page<ProjectInfo> searchManaged(SearchManagedProjectQuery query, Long memberId) {
        Set<ProjectStatus> requested = Set.of(
            ProjectStatus.PENDING_REVIEW,
            ProjectStatus.IN_PROGRESS,
            ProjectStatus.COMPLETED,
            ProjectStatus.ABORTED
        );
        ProjectAccessScope scope = scopeResolver.resolveForManagement(
            memberId, query.gisuId(), requested);

        SearchProjectQuery base = SearchProjectQuery.builder()
            .gisuId(query.gisuId())
            .keyword(query.keyword())
            .statuses(new ArrayList<>(requested))
            .pageable(query.pageable())
            .build();
        return toProjectInfoPage(applyScope(scope, base));
    }

    @Override
    public Page<ProjectInfo> search(SearchProjectQuery query, Long memberId) {
        Set<ProjectStatus> requestedStatuses = new HashSet<>(query.statuses());
        ProjectAccessScope scope = scopeResolver.resolveForPublicSearch(
            memberId, query.gisuId(), requestedStatuses);
        return toProjectInfoPage(applyScope(scope, query));
    }

    @Override
    public Page<ProjectInfo> search(SearchProjectQuery query, SubjectAttributes subject) {
        Set<ProjectStatus> requestedStatuses = new HashSet<>(query.statuses());
        ProjectAccessScope scope = scopeResolver.resolveForPublicSearch(
            subject, query.gisuId(), requestedStatuses);
        return toProjectInfoPage(applyScope(scope, query));
    }

    /**
     * 결정된 {@link ProjectAccessScope} 를 {@link SearchProjectQuery} 에 반영해 어댑터에 위임한다.
     */
    private Page<Project> applyScope(ProjectAccessScope scope, SearchProjectQuery query) {
        return switch (scope) {
            case ProjectAccessScope.Clauses(List<com.umc.product.project.application.access.ScopeClause> values) ->
                loadProjectPort.search(query.withScopeClauses(values));
            case All(Set<ProjectStatus> statuses) -> loadProjectPort.search(query.withStatuses(statuses));
            case ChapterScoped(Long chapterId, Set<ProjectStatus> statuses) ->
                loadProjectPort.search(query.withChapterFilter(chapterId, statuses));
            case OwnerOnly(Long memberId, Set<ProjectStatus> statuses) ->
                loadProjectPort.search(query.withOwnerFilter(memberId, statuses));
            case ProjectAccessScope.WithOwnerIncluded included ->
                loadProjectPort.search(toScopedQuery(included.baseScope(), query)
                    .withIncludedOwner(included.ownerMemberId(), included.ownerVisibleStatuses()));
            case PublicOnly() -> loadProjectPort.search(query.withStatuses(
                Set.of(ProjectStatus.IN_PROGRESS, ProjectStatus.COMPLETED)));
            case None() -> new PageImpl<>(List.of(), query.pageable(), 0L);
        };
    }

    private SearchProjectQuery toScopedQuery(ProjectAccessScope scope, SearchProjectQuery query) {
        return switch (scope) {
            case ProjectAccessScope.Clauses(List<com.umc.product.project.application.access.ScopeClause> values) ->
                query.withScopeClauses(values);
            case All(Set<ProjectStatus> statuses) -> query.withStatuses(statuses);
            case ChapterScoped(Long chapterId, Set<ProjectStatus> statuses) ->
                query.withChapterFilter(chapterId, statuses);
            case OwnerOnly(Long memberId, Set<ProjectStatus> statuses) ->
                query.withOwnerFilter(memberId, statuses);
            case PublicOnly() -> query.withStatuses(Set.of(ProjectStatus.IN_PROGRESS, ProjectStatus.COMPLETED));
            case None() -> query.withStatuses(Set.of(ProjectStatus.IN_PROGRESS));
            case ProjectAccessScope.WithOwnerIncluded included ->
                toScopedQuery(included.baseScope(), query)
                    .withIncludedOwner(included.ownerMemberId(), included.ownerVisibleStatuses());
        };
    }

    /**
     * Project 페이지를 ProjectInfo 페이지로 배치 조립합니다.
     * <p>
     * coPM/파트별 TO/파일 URL 을 페이지 단위 IN 쿼리로 한 번에 조회해 N+1 을 방지합니다.
     */
    private Page<ProjectInfo> toProjectInfoPage(Page<Project> page) {
        List<Project> projects = page.getContent();
        if (projects.isEmpty()) {
            return new PageImpl<>(List.of(), page.getPageable(), page.getTotalElements());
        }

        Set<Long> projectIds = projects.stream()
            .map(Project::getId)
            .collect(Collectors.toSet());

        Map<Long, List<ProjectMember>> planMembersByProject =
            loadProjectMemberPort.listByProjectIdsAndPartGroupedByProjectId(projectIds, ChallengerPart.PLAN);
        Map<Long, List<ProjectPartQuota>> quotasByProject =
            loadProjectPartQuotaPort.listByProjectIdsGroupedByProjectId(projectIds);
        Map<Long, Map<ChallengerPart, Long>> memberCountsByProject =
            loadProjectMemberPort.countByProjectIdsGroupByProjectIdAndPart(projectIds);
        Map<String, String> fileLinks = resolveFileLinks(projects);

        return page.map(project -> assembleProjectInfo(
            project,
            planMembersByProject.getOrDefault(project.getId(), List.of()),
            quotasByProject.getOrDefault(project.getId(), List.of()),
            memberCountsByProject.getOrDefault(project.getId(), Map.of()),
            fileLinks
        ));
    }

    /**
     * 미리 배치 조회한 결과로 단일 Project 의 ProjectInfo 를 조립합니다.
     */
    private ProjectInfo assembleProjectInfo(
        Project project,
        List<ProjectMember> planMembers,
        List<ProjectPartQuota> quotas,
        Map<ChallengerPart, Long> currentCounts,
        Map<String, String> fileLinks
    ) {
        List<Long> coPmMemberIds = planMembers.stream()
            .map(ProjectMember::getMemberId)
            .filter(memberId -> !memberId.equals(project.getProductOwnerMemberId()))
            .toList();

        List<ProjectPartQuotaInfo> partQuotas = quotas.stream()
            .map(q -> ProjectPartQuotaInfo.of(
                q.getPart(),
                q.getQuota(),
                currentCounts.getOrDefault(q.getPart(), 0L)
            ))
            .toList();

        return ProjectInfo.from(
            project,
            coPmMemberIds,
            partQuotas,
            resolveLink(fileLinks, project.getThumbnailFileId()),
            resolveLink(fileLinks, project.getLogoFileId())
        );
    }

    /**
     * 여러 프로젝트의 썸네일/로고 파일 ID → CDN URL을 일괄 조회합니다.
     */
    private Map<String, String> resolveFileLinks(List<Project> projects) {
        List<String> fileIds = projects.stream()
            .flatMap(p -> Stream.of(p.getThumbnailFileId(), p.getLogoFileId()))
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        if (fileIds.isEmpty()) {
            return Map.of();
        }

        return getFileUseCase.getFileLinks(fileIds);
    }

    /**
     * Project 엔티티를 ProjectInfo로 조립합니다. coPM 목록, 파트별 TO, 파일 URL을 함께 조회합니다.
     */
    private ProjectInfo toProjectInfo(Project project) {
        List<Long> coPmMemberIds = extractCoPmMemberIds(project);
        List<ProjectPartQuotaInfo> partQuotas = buildPartQuotas(project.getId());
        Map<String, String> fileLinks = resolveFileLinks(project);

        return ProjectInfo.from(
            project,
            coPmMemberIds,
            partQuotas,
            resolveLink(fileLinks, project.getThumbnailFileId()),
            resolveLink(fileLinks, project.getLogoFileId())
        );
    }

    private String resolveLink(Map<String, String> fileLinks, String fileId) {
        return fileId == null ? null : fileLinks.get(fileId);
    }

    /**
     * PLAN 파트 멤버 중 메인 PO를 제외한 보조 PM Member ID 목록을 추출합니다.
     */
    private List<Long> extractCoPmMemberIds(Project project) {
        List<ProjectMember> planMembers = loadProjectMemberPort
            .listByProjectIdAndPart(project.getId(), ChallengerPart.PLAN);

        return planMembers.stream()
            .map(ProjectMember::getMemberId)
            .filter(memberId -> !memberId.equals(project.getProductOwnerMemberId()))
            .toList();
    }

    /**
     * 파트별 TO 정보를 조립합니다 (quota + 현재 활성 멤버 수).
     */
    private List<ProjectPartQuotaInfo> buildPartQuotas(Long projectId) {
        List<ProjectPartQuota> quotas = loadProjectPartQuotaPort.listByProjectId(projectId);
        if (quotas.isEmpty()) {
            return List.of();
        }

        Map<ChallengerPart, Long> currentCounts = loadProjectMemberPort
            .countByProjectIdGroupByPart(projectId);

        return quotas.stream()
            .map(q -> ProjectPartQuotaInfo.of(
                q.getPart(),
                q.getQuota(),
                currentCounts.getOrDefault(q.getPart(), 0L)
            ))
            .toList();
    }

    /**
     * 썸네일/로고 파일 ID → CDN URL을 일괄 조회합니다.
     */
    private Map<String, String> resolveFileLinks(Project project) {
        List<String> fileIds = new ArrayList<>();
        if (project.getThumbnailFileId() != null) {
            fileIds.add(project.getThumbnailFileId());
        }
        if (project.getLogoFileId() != null) {
            fileIds.add(project.getLogoFileId());
        }

        if (fileIds.isEmpty()) {
            return Map.of();
        }

        return getFileUseCase.getFileLinks(fileIds);
    }
}
