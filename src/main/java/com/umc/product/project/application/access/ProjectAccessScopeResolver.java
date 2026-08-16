package com.umc.product.project.application.access;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.project.application.access.ProjectAccessScope.All;
import com.umc.product.project.application.access.ProjectAccessScope.ChapterScoped;
import com.umc.product.project.application.access.ProjectAccessScope.None;
import com.umc.product.project.application.access.ProjectAccessScope.OwnerOnly;
import com.umc.product.project.application.access.ProjectAccessScope.PublicOnly;
import com.umc.product.project.application.access.ProjectAccessScope.WithOwnerIncluded;
import com.umc.product.project.application.port.out.LoadProjectPort;
import com.umc.product.project.domain.enums.ProjectStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 호출 컨텍스트(공개 검색 vs 관리 화면) + 사용자 역할에 따라 {@link ProjectAccessScope} 를 결정한다.
 * <p>
 * 같은 사용자라도 호출 의도에 따라 결과가 달라야 하므로 {@code resolveForPublicSearch} /
 * {@code resolveForManagement} 두 메서드로 명시적으로 분기한다.
 */
@Component
@RequiredArgsConstructor
public class ProjectAccessScopeResolver {

    private final GetChallengerRoleUseCase getChallengerRoleUseCase;
    private final LoadProjectPort loadProjectPort;
    private final GetChapterUseCase getChapterUseCase;

    /**
     * 공개 검색(PROJECT-001) 컨텍스트.
     * <p>
     * 권한 별 노출 가능 status:
     * <ul>
     *   <li>SUPER_ADMIN ∪ 총괄단(총괄/부총괄) ∪ 지부장: DRAFT 제외 전체 (PR/IP/COMPLETED/ABORTED)</li>
     *   <li>그 외(일반 챌린저, 학교 회장단): 공개 status (IN_PROGRESS / COMPLETED)</li>
     *   <li>비회원(memberId == null): 공개 status (IN_PROGRESS / COMPLETED)</li>
     * </ul>
     * 호출자가 본인 권한 외 status 를 요청하면 {@link ProjectErrorCode#PROJECT_ACCESS_DENIED} 로 거부한다.
     */
    public ProjectAccessScope resolveForPublicSearch(
        Long memberId, Long gisuId, Set<ProjectStatus> requestedStatuses
    ) {
        if (memberId == null) {
            return publicOnlyOrDeny(requestedStatuses);
        }

        if (getChallengerRoleUseCase.isSuperAdmin(memberId)) {
            if (requestedStatuses.contains(ProjectStatus.DRAFT)) {
                throw new ProjectDomainException(ProjectErrorCode.PROJECT_ACCESS_DENIED);
            }
            return new All(requestedStatuses);
        }

        List<ChallengerRoleInfo> rolesInGisu = getChallengerRoleUseCase.findAllByMemberId(memberId).stream()
            .filter(role -> Objects.equals(role.gisuId(), gisuId))
            .toList();

        boolean isStaff = rolesInGisu.stream()
            .anyMatch(role -> role.roleType().isAtLeastCentralCore()
                || role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT);

        if (isStaff) {
            if (requestedStatuses.contains(ProjectStatus.DRAFT)) {
                throw new ProjectDomainException(ProjectErrorCode.PROJECT_ACCESS_DENIED);
            }
            return new All(requestedStatuses);
        }

        return publicOnlyOrDeny(requestedStatuses);
    }

    private ProjectAccessScope publicOnlyOrDeny(Set<ProjectStatus> requestedStatuses) {
        boolean publicAllowed = requestedStatuses.stream()
            .allMatch(s -> s == ProjectStatus.IN_PROGRESS || s == ProjectStatus.COMPLETED);
        if (!publicAllowed) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_ACCESS_DENIED);
        }
        return new PublicOnly();
    }

    /**
     * 관리 화면(PROJECT-006) 컨텍스트. 역할별 scope 차등 적용.
     * <ol>
     *   <li>Central Core → 전체 (DRAFT 제외)</li>
     *   <li>지부장 → 본인 지부 (DRAFT 제외)</li>
     *   <li>학교 회장단 → 본인 학교가 속한 지부 전체 (DRAFT 제외) — 정책상 지부장과 동일 범위</li>
     *   <li>PM 챌린저 → 본인이 owner 인 프로젝트만 (DRAFT 포함)</li>
     *   <li>그 외 → 관리 대상 0건</li>
     * </ol>
     * <p>
     * {@code requestedStatuses} 는 운영진 분기에 그대로 전달되며, PO 분기에서는 DRAFT 가 union 된다.
     */
    public ProjectAccessScope resolveForManagement(
        Long memberId, Long gisuId, Set<ProjectStatus> requestedStatuses
    ) {
        if (getChallengerRoleUseCase.isSuperAdmin(memberId)) {
            return includeOwnerProjects(new All(requestedStatuses), memberId, gisuId, requestedStatuses);
        }

        List<ChallengerRoleInfo> rolesInGisu = getChallengerRoleUseCase.findAllByMemberId(memberId).stream()
            .filter(role -> Objects.equals(role.gisuId(), gisuId))
            .toList();

        if (rolesInGisu.stream().anyMatch(r -> r.roleType().isAtLeastCentralCore())) {
            return includeOwnerProjects(new All(requestedStatuses), memberId, gisuId, requestedStatuses);
        }

        Optional<Long> chapterId = chapterPresidentOrgId(rolesInGisu);
        if (chapterId.isPresent()) {
            return includeOwnerProjects(new ChapterScoped(chapterId.get(), requestedStatuses),
                memberId, gisuId, requestedStatuses);
        }

        Optional<Long> schoolId = schoolCoreOrgId(rolesInGisu);
        if (schoolId.isPresent()) {
            Long schoolChapterId = getChapterUseCase.byGisuAndSchool(gisuId, schoolId.get()).id();
            return includeOwnerProjects(new ChapterScoped(schoolChapterId, requestedStatuses),
                memberId, gisuId, requestedStatuses);
        }

        if (loadProjectPort.existsByOwnerAndGisu(memberId, gisuId)) {
            Set<ProjectStatus> withDraft = requestedStatuses.isEmpty()
                ? EnumSet.allOf(ProjectStatus.class)
                : EnumSet.copyOf(requestedStatuses);
            withDraft.add(ProjectStatus.DRAFT);
            return new OwnerOnly(memberId, withDraft);
        }

        return new None();
    }

    private ProjectAccessScope includeOwnerProjects(
        ProjectAccessScope baseScope,
        Long memberId,
        Long gisuId,
        Set<ProjectStatus> requestedStatuses
    ) {
        if (!loadProjectPort.existsByOwnerAndGisu(memberId, gisuId)) {
            return baseScope;
        }

        Set<ProjectStatus> ownerStatuses = requestedStatuses.isEmpty()
            ? EnumSet.allOf(ProjectStatus.class)
            : EnumSet.copyOf(requestedStatuses);
        ownerStatuses.add(ProjectStatus.DRAFT);
        return new WithOwnerIncluded(baseScope, memberId, ownerStatuses);
    }

    private Optional<Long> chapterPresidentOrgId(List<ChallengerRoleInfo> rolesInGisu) {
        return rolesInGisu.stream()
            .filter(role -> role.roleType() == ChallengerRoleType.CHAPTER_PRESIDENT)
            .map(ChallengerRoleInfo::organizationId)
            .findFirst();
    }

    private Optional<Long> schoolCoreOrgId(List<ChallengerRoleInfo> rolesInGisu) {
        return rolesInGisu.stream()
            .filter(role -> role.roleType() == ChallengerRoleType.SCHOOL_PRESIDENT
                || role.roleType() == ChallengerRoleType.SCHOOL_VICE_PRESIDENT)
            .map(ChallengerRoleInfo::organizationId)
            .findFirst();
    }
}
