package com.umc.product.project.application.access;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.project.application.access.ProjectApplicationAccessScope.All;
import com.umc.product.project.application.access.ProjectApplicationAccessScope.AllInGisu;
import com.umc.product.project.application.access.ProjectApplicationAccessScope.ChapterScoped;
import com.umc.product.project.application.access.ProjectApplicationAccessScope.None;
import com.umc.product.project.application.access.ProjectApplicationAccessScope.OwnerOnly;
import com.umc.product.project.application.access.ProjectApplicationAccessScope.ProjectScoped;
import com.umc.product.project.application.port.out.LoadProjectMemberPort;
import com.umc.product.project.domain.Project;

import lombok.RequiredArgsConstructor;

/**
 * 호출 컨텍스트(본인 지원 내역 vs PO 검토 vs 운영진 모니터링) + 사용자 역할에 따라 {@link ProjectApplicationAccessScope} 를 결정한다.
 */
@Component
@RequiredArgsConstructor
public class ProjectApplicationAccessScopeResolver {

    private final GetChallengerRoleUseCase getChallengerRoleUseCase;
    private final LoadProjectMemberPort loadProjectMemberPort;
    private final GetChapterUseCase getChapterUseCase;

    /**
     * 본인 지원 내역 화면. 누구든지 본인 지원서만 본다.
     */
    public ProjectApplicationAccessScope resolveForApplicant(Long memberId) {
        return new OwnerOnly(memberId);
    }

    /**
     * 단일 프로젝트 지원자 목록(APPLY-101) 화면. 호출자가 다음 중 하나라도 만족하면 {@link ProjectScoped} 통과:
     * <ul>
     *   <li>해당 프로젝트의 PO</li>
     *   <li>해당 프로젝트의 보조 PM (ACTIVE PLAN 멤버)</li>
     *   <li>SUPER_ADMIN</li>
     *   <li>해당 프로젝트 기수의 Central Core (총괄/부총괄)</li>
     *   <li>해당 프로젝트 지부의 지부장 (같은 기수)</li>
     *   <li>해당 프로젝트 지부에 속한 학교 회장단 (SCHOOL_PRESIDENT/SCHOOL_VICE_PRESIDENT, 같은 기수)</li>
     * </ul>
     * 그 외엔 {@link None} 반환 — 호출 측이 빈 목록 처리하여 권한 부재를 '지원자 0건' 으로 위장한다.
     * <p>
     * {@code project} 는 호출자(서비스 단)가 사전 로드한 인스턴스를 그대로 전달받는다. 동일 트랜잭션 내 중복 조회를 피하기 위함.
     */
    public ProjectApplicationAccessScope resolveForProjectApplicantList(Long memberId, Project project) {
        Long projectId = project.getId();

        if (Objects.equals(project.getProductOwnerMemberId(), memberId)
            || loadProjectMemberPort.isActivePlanMember(projectId, memberId)) {
            return new ProjectScoped(projectId);
        }

        if (getChallengerRoleUseCase.isSuperAdmin(memberId)) {
            return new ProjectScoped(projectId, true);
        }

        List<ChallengerRoleInfo> roles = getChallengerRoleUseCase.findAllByMemberId(memberId);
        List<ChallengerRoleInfo> rolesInGisu = roles.stream()
            .filter(r -> Objects.equals(r.gisuId(), project.getGisuId()))
            .toList();

        if (rolesInGisu.stream().anyMatch(r -> r.roleType().isAtLeastCentralCore())) {
            return new ProjectScoped(projectId, true);
        }

        if (rolesInGisu.stream().anyMatch(r -> r.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT
            && Objects.equals(r.organizationId(), project.getChapterId()))) {
            return new ProjectScoped(projectId);
        }

        if (isSchoolCoreInProjectChapter(rolesInGisu, project)) {
            return new ProjectScoped(projectId);
        }

        return new None();
    }

    /**
     * 복수 프로젝트 지원자 목록(APPLY-101 batch) 화면의 프로젝트별 scope 를 한 번에 결정한다.
     * <p>
     * 단건 판정의 우선순위(PO/Sub-PM -> SUPER_ADMIN -> Central Core -> 지부장 -> 학교 회장단)를 유지하되, 역할/보조 PM/학교-지부 매핑 조회를 batch 로
     * 수행한다.
     */
    public Map<Long, ProjectApplicationAccessScope> resolveForProjectApplicantLists(
        Long memberId,
        Collection<Project> projects
    ) {
        if (projects == null || projects.isEmpty()) {
            return Map.of();
        }

        Set<Long> projectIds = projects.stream()
            .map(Project::getId)
            .collect(Collectors.toSet());
        Set<Long> activePlanProjectIds = new HashSet<>(
            loadProjectMemberPort.listProjectIdsByActivePlanMember(projectIds, memberId));

        boolean superAdmin = getChallengerRoleUseCase.isSuperAdmin(memberId);
        List<ChallengerRoleInfo> roles = superAdmin
            ? List.of()
            : getChallengerRoleUseCase.findAllByMemberId(memberId);

        Set<Long> gisuIds = projects.stream()
            .map(Project::getGisuId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Set<Long> schoolIds = roles.stream()
            .filter(r -> r.roleType() == ChallengerRoleType.SCHOOL_PRESIDENT
                || r.roleType() == ChallengerRoleType.SCHOOL_VICE_PRESIDENT)
            .map(ChallengerRoleInfo::organizationId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, Map<Long, ChapterInfo>> chapterByGisuAndSchool = schoolIds.isEmpty() || gisuIds.isEmpty()
            ? Map.of()
            : getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(gisuIds, schoolIds);

        Map<Long, ProjectApplicationAccessScope> result = new LinkedHashMap<>();
        for (Project project : projects) {
            Long projectId = project.getId();
            if (Objects.equals(project.getProductOwnerMemberId(), memberId)
                || activePlanProjectIds.contains(projectId)) {
                result.put(projectId, new ProjectScoped(projectId));
                continue;
            }

            if (superAdmin) {
                result.put(projectId, new ProjectScoped(projectId, true));
                continue;
            }

            List<ChallengerRoleInfo> rolesInGisu = roles.stream()
                .filter(r -> Objects.equals(r.gisuId(), project.getGisuId()))
                .toList();

            if (rolesInGisu.stream().anyMatch(r -> r.roleType().isAtLeastCentralCore())) {
                result.put(projectId, new ProjectScoped(projectId, true));
                continue;
            }

            if (rolesInGisu.stream().anyMatch(r -> r.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT
                && Objects.equals(r.organizationId(), project.getChapterId()))) {
                result.put(projectId, new ProjectScoped(projectId));
                continue;
            }

            if (isSchoolCoreInProjectChapter(rolesInGisu, project, chapterByGisuAndSchool)) {
                result.put(projectId, new ProjectScoped(projectId));
                continue;
            }

            result.put(projectId, new None());
        }
        return result;
    }

    private boolean isSchoolCoreInProjectChapter(List<ChallengerRoleInfo> rolesInGisu, Project project) {
        if (project.getGisuId() == null) {
            return false;
        }

        Set<Long> schoolIds = rolesInGisu.stream()
            .filter(r -> r.roleType() == ChallengerRoleType.SCHOOL_PRESIDENT
                || r.roleType() == ChallengerRoleType.SCHOOL_VICE_PRESIDENT)
            .map(ChallengerRoleInfo::organizationId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (schoolIds.isEmpty()) {
            return false;
        }

        Map<Long, Map<Long, ChapterInfo>> chapterByGisuAndSchool =
            getChapterUseCase.getChapterMapByGisuIdsAndSchoolIds(Set.of(project.getGisuId()), schoolIds);
        Map<Long, ChapterInfo> chapterBySchool =
            chapterByGisuAndSchool.getOrDefault(project.getGisuId(), Map.of());

        return chapterBySchool.values().stream()
            .filter(Objects::nonNull)
            .anyMatch(chapter -> Objects.equals(chapter.id(), project.getChapterId()));
    }

    private boolean isSchoolCoreInProjectChapter(
        List<ChallengerRoleInfo> rolesInGisu,
        Project project,
        Map<Long, Map<Long, ChapterInfo>> chapterByGisuAndSchool
    ) {
        if (project.getGisuId() == null) {
            return false;
        }

        Set<Long> schoolIds = rolesInGisu.stream()
            .filter(r -> r.roleType() == ChallengerRoleType.SCHOOL_PRESIDENT
                || r.roleType() == ChallengerRoleType.SCHOOL_VICE_PRESIDENT)
            .map(ChallengerRoleInfo::organizationId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (schoolIds.isEmpty()) {
            return false;
        }

        Map<Long, ChapterInfo> chapterBySchool =
            chapterByGisuAndSchool.getOrDefault(project.getGisuId(), Map.of());

        return schoolIds.stream()
            .map(chapterBySchool::get)
            .filter(Objects::nonNull)
            .anyMatch(chapter -> Objects.equals(chapter.id(), project.getChapterId()));
    }

    /**
     * 운영진 모니터링 화면.
     * <ol>
     *   <li>SUPER_ADMIN → {@link All}</li>
     *   <li>해당 기수 총괄단 → {@link AllInGisu}</li>
     *   <li>해당 기수 지부장 → {@link ChapterScoped}</li>
     *   <li>그 외 → {@link None}</li>
     * </ol>
     */
    public ProjectApplicationAccessScope resolveForManagement(Long memberId, Long gisuId) {
        if (getChallengerRoleUseCase.isSuperAdmin(memberId)) {
            return new All();
        }

        List<ChallengerRoleInfo> roles = getChallengerRoleUseCase.findAllByMemberId(memberId);
        List<ChallengerRoleInfo> rolesInGisu = roles.stream()
            .filter(r -> Objects.equals(r.gisuId(), gisuId))
            .toList();

        if (rolesInGisu.stream().anyMatch(r -> r.roleType().isAtLeastCentralCore())) {
            return new AllInGisu(gisuId);
        }

        List<Long> chapterIds = rolesInGisu.stream()
            .filter(r -> r.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT)
            .map(ChallengerRoleInfo::organizationId)
            .toList();
        if (!chapterIds.isEmpty()) {
            return new ChapterScoped(chapterIds, gisuId);
        }

        return new None();
    }
}
