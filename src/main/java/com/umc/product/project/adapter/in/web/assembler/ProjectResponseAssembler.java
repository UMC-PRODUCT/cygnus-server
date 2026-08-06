package com.umc.product.project.adapter.in.web.assembler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.global.response.PageResponse;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.project.adapter.in.web.dto.common.MemberBrief;
import com.umc.product.project.adapter.in.web.dto.response.DraftProjectResponse;
import com.umc.product.project.adapter.in.web.dto.response.ManagedProjectSummaryResponse;
import com.umc.product.project.adapter.in.web.dto.response.ProjectDetailResponse;
import com.umc.product.project.adapter.in.web.dto.response.ProjectMembersResponse;
import com.umc.product.project.adapter.in.web.dto.response.ProjectMembersResponse.MatchedRoundInfo;
import com.umc.product.project.adapter.in.web.dto.response.ProjectMembersResponse.PartGroup;
import com.umc.product.project.adapter.in.web.dto.response.ProjectMembersResponse.ProjectMemberBrief;
import com.umc.product.project.adapter.in.web.dto.response.ProjectSummaryResponse;
import com.umc.product.project.adapter.in.web.dto.response.statistics.ChapterProjectMatchingStatisticsResponse;
import com.umc.product.project.adapter.in.web.dto.response.statistics.ChapterProjectStatisticsResponse;
import com.umc.product.project.adapter.in.web.dto.response.statistics.ProjectStatisticsResponse;
import com.umc.product.project.application.port.in.query.GetProjectStatisticsUseCase;
import com.umc.product.project.application.port.in.query.GetProjectUseCase;
import com.umc.product.project.application.port.in.query.SearchManagedProjectUseCase;
import com.umc.product.project.application.port.in.query.SearchProjectUseCase;
import com.umc.product.project.application.port.in.query.dto.ProjectInfo;
import com.umc.product.project.application.port.in.query.dto.SearchManagedProjectQuery;
import com.umc.product.project.application.port.in.query.dto.SearchProjectQuery;
import com.umc.product.project.application.port.out.LoadProjectApplicationFormPort;
import com.umc.product.project.application.port.out.LoadProjectApplicationPort;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.application.port.out.dto.ProjectMemberMatchedRoundInfo;
import com.umc.product.project.domain.ProjectApplicationForm;
import com.umc.product.project.domain.ProjectMember;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Project Response 조립기. Controller에서 여러 UseCase를 조합하는 로직을 캡슐화합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectResponseAssembler {

    private final GetProjectUseCase getProjectUseCase;
    private final SearchProjectUseCase searchProjectUseCase;
    private final SearchManagedProjectUseCase searchManagedProjectUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final LoadProjectApplicationFormPort loadProjectApplicationFormPort;
    private final LoadProjectApplicationPort loadProjectApplicationPort;
    private final LoadProjectMemberPort loadProjectMemberPort;
    private final GetProjectStatisticsUseCase getProjectStatisticsUseCase;
    private final CheckPermissionUseCase checkPermissionUseCase;

    /**
     * PROJECT-001 프로젝트 목록 조회.
     */
    public PageResponse<ProjectSummaryResponse> searchFor(
        SearchProjectQuery query,
        Long requesterMemberId
    ) {
        Page<ProjectInfo> page = searchProjectUseCase.search(query, requesterMemberId);

        Set<Long> ownerIds = page.getContent().stream()
            .map(ProjectInfo::productOwnerMemberId)
            .collect(Collectors.toSet());
        Map<Long, MemberInfo> memberMap = ownerIds.isEmpty()
            ? Map.of()
            : getMemberUseCase.findAllByIds(ownerIds);

        return PageResponse.of(page, info -> {
            MemberBrief owner = toPublicBrief(memberMap.get(info.productOwnerMemberId()));
            return ProjectSummaryResponse.from(info, owner);
        });
    }

    /**
     * PROJECT-002 프로젝트 상세 조회.
     */
    public ProjectDetailResponse detailFor(Long projectId) {
        ProjectInfo info = getProjectUseCase.getById(projectId);

        Map<Long, MemberInfo> memberMap = loadMembers(info);

        MemberBrief owner = toPublicBrief(memberMap.get(info.productOwnerMemberId()));
        List<MemberBrief> coOwners = info.coProductOwnerMemberIds().stream()
            .map(id -> toPublicBrief(memberMap.get(id)))
            .toList();

        return ProjectDetailResponse.from(info, owner, coOwners, resolveApplicationFormId(projectId));
    }

    /**
     * PROJECT-006 관리 화면 프로젝트 목록.
     * <p>
     * 호출자 역할에 따라 자동 scope 적용 (Service 단 {@code resolveForManagement}).
     */
    public PageResponse<ManagedProjectSummaryResponse> searchManagedFor(
        SearchManagedProjectQuery query,
        Long requesterMemberId
    ) {
        Page<ProjectInfo> page = searchManagedProjectUseCase.searchManaged(query, requesterMemberId);

        Set<Long> ownerIds = page.getContent().stream()
            .map(ProjectInfo::productOwnerMemberId)
            .collect(Collectors.toSet());
        Map<Long, MemberInfo> memberMap = ownerIds.isEmpty()
            ? Map.of()
            : getMemberUseCase.findAllByIds(ownerIds);

        return PageResponse.of(page, info -> {
            MemberBrief owner = toBrief(memberMap.get(info.productOwnerMemberId()));
            return ManagedProjectSummaryResponse.from(info, owner);
        });
    }

    /**
     * PROJECT-003 프로젝트 팀원 구성 조회.
     * <p>
     * 메인 PM 별도 노출 + 보조 PM(PLAN 파트의 다른 멤버) + 그 외 파트별 그룹. 각 그룹은 ProjectMember 생성일 오름차순 정렬.
     */
    public ProjectMembersResponse membersFor(Long projectId) {
        ProjectInfo info = getProjectUseCase.getById(projectId);
        List<ProjectMember> members = loadProjectMemberPort.listByProjectId(projectId);

        Set<Long> memberIds = members.stream().map(ProjectMember::getMemberId).collect(Collectors.toSet());
        memberIds.add(info.productOwnerMemberId());
        Map<Long, MemberInfo> memberMap = memberIds.isEmpty()
            ? Map.of()
            : getMemberUseCase.findAllByIds(memberIds);
        Map<ProjectMemberKey, MatchedRoundInfo> matchedRoundMap =
            loadMatchedRoundMap(Set.of(projectId), memberIds);

        return buildMembersResponse(projectId, info, members, memberMap, matchedRoundMap);
    }

    /**
     * PROJECT-007 프로젝트 팀원 구성 일괄 조회.
     * <p>
     * 각 projectId에 대해 per-project 권한 체크 및 데이터 조회를 수행하며, 실패한 프로젝트는 결과에서 제외 멤버 정보는 유효한 프로젝트 전체를 모아 한 번에 조회한다.
     */
    public Map<Long, ProjectMembersResponse> listProjectMembers(List<Long> projectIds, Long memberId) {
        // Step 1: 권한 체크 + 프로젝트 정보 조회 (실패 시 skip)
        Map<Long, ProjectInfo> validProjects = new LinkedHashMap<>();
        for (Long projectId : projectIds) {
            boolean hasAccess = checkPermissionUseCase.check(
                memberId, ResourcePermission.of(ResourceType.PROJECT, projectId, PermissionType.READ));
            if (!hasAccess) {
                log.warn("프로젝트 팀원 일괄 조회 - 접근 권한 없음: memberId={}, projectId={}", memberId, projectId);
                continue;
            }
            try {
                validProjects.put(projectId, getProjectUseCase.getById(projectId));
            } catch (Exception e) {
                log.warn("프로젝트 팀원 일괄 조회 - 프로젝트 조회 실패: projectId={}, reason={}", projectId, e.getMessage());
            }
        }

        if (validProjects.isEmpty()) {
            return Map.of();
        }

        // Step 2: 유효한 전체 프로젝트의 파트 멤버를 IN 쿼리 한 번으로 조회 후 메모리 grouping
        Map<Long, List<ProjectMember>> projectMembersMap =
            loadProjectMemberPort.listByProjectIds(validProjects.keySet());

        // Step 3: 유효한 전체 프로젝트의 멤버 ID를 모아 한 번에 조회
        // TODO: getProjectUseCase.getById() N+1 — validProjects 수만큼 호출됨.
        //       GetProjectUseCase.listByIds() 배치 메서드 추가 후 개선 필요.
        Set<Long> allMemberIds = new HashSet<>();
        validProjects.forEach((projectId, info) -> {
            allMemberIds.add(info.productOwnerMemberId());
            projectMembersMap.getOrDefault(projectId, List.of())
                .forEach(m -> allMemberIds.add(m.getMemberId()));
        });
        Map<Long, MemberInfo> memberMap = allMemberIds.isEmpty()
            ? Map.of()
            : getMemberUseCase.findAllByIds(allMemberIds);
        Map<ProjectMemberKey, MatchedRoundInfo> matchedRoundMap =
            loadMatchedRoundMap(validProjects.keySet(), allMemberIds);

        // Step 4: 응답 조립
        Map<Long, ProjectMembersResponse> result = new LinkedHashMap<>();
        validProjects.forEach((projectId, info) ->
            result.put(projectId, buildMembersResponse(
                projectId,
                info,
                projectMembersMap.getOrDefault(projectId, List.of()),
                memberMap,
                matchedRoundMap)));

        return result;
    }

    /**
     * PROJECT-103 내가 작성 중인 Draft 조회 (creator 기준). Draft 가 없으면 {@code null} 반환.
     */
    public DraftProjectResponse draftFor(Long memberId, Long gisuId) {
        Optional<ProjectInfo> maybe = getProjectUseCase.findDraftByCreatorAndGisu(memberId, gisuId);
        if (maybe.isEmpty()) {
            return null;
        }
        ProjectInfo info = maybe.get();

        Map<Long, MemberInfo> memberMap = loadMembers(info);

        MemberBrief owner = toBrief(memberMap.get(info.productOwnerMemberId()));
        List<MemberBrief> coOwners = info.coProductOwnerMemberIds().stream()
            .map(id -> toBrief(memberMap.get(id)))
            .toList();

        return DraftProjectResponse.from(info, owner, coOwners, resolveApplicationFormId(info.id()));
    }

    /**
     * PROJECT-STAT-001 단건 프로젝트 지원/매칭 현황.
     */
    public ProjectStatisticsResponse statisticsForProject(Long projectId, Long requesterMemberId) {
        return ProjectStatisticsResponse.from(
            getProjectStatisticsUseCase.getByProjectId(projectId, requesterMemberId));
    }

    /**
     * PROJECT-STAT-002 지부 전체 프로젝트 지원/매칭 현황.
     */
    public ChapterProjectStatisticsResponse statisticsForChapter(Long chapterId, Long requesterMemberId) {
        return ChapterProjectStatisticsResponse.from(
            getProjectStatisticsUseCase.getByChapterId(chapterId, requesterMemberId));
    }

    /**
     * PROJECT-STAT-002 프로젝트 목록 지원/매칭 현황.
     */
    public ChapterProjectStatisticsResponse statisticsForProjects(List<Long> projectIds, Long requesterMemberId) {
        return ChapterProjectStatisticsResponse.from(
            getProjectStatisticsUseCase.getByProjectIds(projectIds, requesterMemberId));
    }

    /**
     * PROJECT-STAT-003 지부 공개 프로젝트 매칭 요약.
     */
    public ChapterProjectMatchingStatisticsResponse matchingStatisticsForChapter(Long chapterId) {
        return ChapterProjectMatchingStatisticsResponse.from(
            getProjectStatisticsUseCase.getPublicMatchingStatisticsByChapterId(chapterId));
    }

    private ProjectMembersResponse buildMembersResponse(
        Long projectId,
        ProjectInfo info,
        List<ProjectMember> members,
        Map<Long, MemberInfo> memberMap,
        Map<ProjectMemberKey, MatchedRoundInfo> matchedRoundMap
    ) {
        ProjectMemberBrief productOwner = toProjectMemberBrief(
            memberMap.get(info.productOwnerMemberId()),
            matchedRoundMap.get(ProjectMemberKey.of(projectId, info.productOwnerMemberId()))
        );

        Map<ChallengerPart, List<ProjectMemberBrief>> partToMembers = new EnumMap<>(ChallengerPart.class);
        List<ProjectMember> orderedMembers = members.stream()
            .sorted(Comparator.comparing(ProjectMember::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ProjectMember::getId, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

        for (ProjectMember m : orderedMembers) {
            ProjectMemberBrief brief = toProjectMemberBrief(
                memberMap.get(m.getMemberId()),
                matchedRoundMap.get(ProjectMemberKey.of(projectId, m.getMemberId()))
            );
            if (brief == null) {
                continue;
            }
            partToMembers.computeIfAbsent(m.getPart(), p -> new ArrayList<>()).add(brief);
        }

        List<ProjectMemberBrief> coProductOwners = partToMembers.getOrDefault(ChallengerPart.PLAN, List.of()).stream()
            .filter(b -> !Objects.equals(b.memberId(), info.productOwnerMemberId()))
            .toList();

        List<PartGroup> partGroups = Arrays.stream(ChallengerPart.values())
            .filter(p -> p != ChallengerPart.PLAN)
            .map(p -> new PartGroup(p,
                partToMembers.getOrDefault(p, List.of())))
            .filter(g -> !g.members().isEmpty())
            .toList();

        return ProjectMembersResponse.builder()
            .projectId(projectId)
            .productOwner(productOwner)
            .coProductOwners(coProductOwners)
            .partGroups(partGroups)
            .build();
    }

    private Map<ProjectMemberKey, MatchedRoundInfo> loadMatchedRoundMap(
        Collection<Long> projectIds,
        Collection<Long> memberIds
    ) {
        if (projectIds == null || projectIds.isEmpty() || memberIds == null || memberIds.isEmpty()) {
            return Map.of();
        }

        List<ProjectMemberMatchedRoundInfo> rows = loadProjectApplicationPort
            .listLatestApprovedMatchedRoundsByProjectIdsAndMemberIds(projectIds, memberIds);
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }

        return rows.stream()
            .collect(Collectors.toMap(
                ProjectMemberKey::from,
                row -> new MatchedRoundInfo(
                    row.matchingRoundId(),
                    row.matchingRoundType(),
                    row.matchingRoundPhase()
                ),
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    private Long resolveApplicationFormId(Long projectId) {
        return loadProjectApplicationFormPort.findByProjectId(projectId)
            .map(ProjectApplicationForm::getId)
            .orElse(null);
    }

    private Map<Long, MemberInfo> loadMembers(ProjectInfo info) {
        Set<Long> ids = new HashSet<>();
        ids.add(info.productOwnerMemberId());
        ids.addAll(info.coProductOwnerMemberIds());
        return ids.isEmpty() ? Map.of() : getMemberUseCase.findAllByIds(ids);
    }

    private MemberBrief toBrief(MemberInfo info) {
        return info == null ? null : MemberBrief.from(info);
    }

    /**
     * PROJECT-001/002 등 비인증 노출 경로에서 사용. 실명·학교명은 요청자 권한과 무관하게 항상 제외한다.
     */
    private MemberBrief toPublicBrief(MemberInfo info) {
        return info == null ? null : MemberBrief.builder()
            .memberId(info.id())
            .nickname(info.nickname())
            .build();
    }

    private ProjectMemberBrief toProjectMemberBrief(MemberInfo info, MatchedRoundInfo matchedRoundInfo) {
        return info == null ? null : ProjectMemberBrief.from(info, matchedRoundInfo);
    }

    private record ProjectMemberKey(Long projectId, Long memberId) {

        private static ProjectMemberKey of(Long projectId, Long memberId) {
            return new ProjectMemberKey(projectId, memberId);
        }

        private static ProjectMemberKey from(ProjectMemberMatchedRoundInfo info) {
            return new ProjectMemberKey(info.projectId(), info.memberId());
        }
    }
}
