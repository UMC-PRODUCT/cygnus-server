package com.umc.product.project.adapter.out.persistence;

import static com.umc.product.project.domain.QProject.project;
import static com.umc.product.project.domain.QProjectApplication.projectApplication;
import static com.umc.product.project.domain.QProjectApplicationForm.projectApplicationForm;
import static com.umc.product.project.domain.QProjectMatchingRound.projectMatchingRound;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.project.application.port.out.dto.ProjectMemberMatchedRoundInfo;
import com.umc.product.project.domain.ProjectApplication;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;

import lombok.RequiredArgsConstructor;

/**
 * ProjectApplication QueryDSL 동적 검색 구현.
 */
@Repository
@RequiredArgsConstructor
public class ProjectApplicationQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * (projectId, applicantMemberId, roundId, status) 조합으로 단건 조회.
     */
    public Optional<ProjectApplication> findByProjectIdAndApplicantMemberIdAndRoundIdAndStatus(
        Long projectId, Long applicantMemberId, Long roundId, ProjectApplicationStatus status
    ) {
        return Optional.ofNullable(
            queryFactory
                .selectFrom(projectApplication)
                .where(
                    projectApplication.applicationForm.project.id.eq(projectId),
                    projectApplication.applicantMemberId.eq(applicantMemberId),
                    projectApplication.appliedMatchingRound.id.eq(roundId),
                    projectApplication.status.eq(status)
                )
                .fetchOne()
        );
    }

    /**
     * 동일 차수에 특정 상태의 지원서가 존재하는지 확인.
     */
    public boolean existsByRoundAndApplicantAndStatus(
        Long roundId, Long applicantMemberId, ProjectApplicationStatus status
    ) {
        return queryFactory
            .selectOne()
            .from(projectApplication)
            .where(
                projectApplication.appliedMatchingRound.id.eq(roundId),
                projectApplication.applicantMemberId.eq(applicantMemberId),
                projectApplication.status.eq(status)
            )
            .fetchFirst() != null;
    }

    /**
     * (projectId, applicantMemberId, status) 조합으로 단건 조회. applicationForm.project.id 를 통해 projectId 까지 타고 들어간다.
     */
    public Optional<ProjectApplication> findByProjectIdAndApplicantMemberIdAndStatus(
        Long projectId, Long applicantMemberId, ProjectApplicationStatus status
    ) {
        return Optional.ofNullable(
            queryFactory
                .selectFrom(projectApplication)
                .where(
                    projectApplication.applicationForm.project.id.eq(projectId),
                    projectApplication.applicantMemberId.eq(applicantMemberId),
                    projectApplication.status.eq(status)
                )
                .fetchOne()
        );
    }

    /**
     * 본인 지원 내역을 조회한다.
     * <p>
     * applicationForm -> project, appliedMatchingRound 를 fetch join 으로 함께 로드한다.
     * <p>
     * 정렬: matching round startsAt ASC -> application updatedAt DESC.
     */
    public List<ProjectApplication> searchMyApplications(
        Long applicantMemberId,
        Long gisuId,
        MatchingType matchingType,
        ProjectApplicationStatus status
    ) {
        return queryFactory
            .selectFrom(projectApplication)
            .innerJoin(projectApplication.applicationForm, projectApplicationForm).fetchJoin()
            .innerJoin(projectApplicationForm.project, project).fetchJoin()
            .innerJoin(projectApplication.appliedMatchingRound, projectMatchingRound).fetchJoin()
            .where(
                projectApplication.applicantMemberId.eq(applicantMemberId),
                project.gisuId.eq(gisuId),
                projectMatchingRound.type.eq(matchingType),
                statusCond(status)
            )
            .orderBy(projectMatchingRound.startsAt.asc(), projectApplication.updatedAt.desc())
            .fetch();
    }

    /**
     * PM/운영진용 단일 프로젝트의 지원자 목록을 조회한다.
     * <p>
     * applicationForm -> project, appliedMatchingRound 를 fetch join 으로 함께 로드한다.
     * 호출자({@code ProjectApplicationSummaryInfo#from})가 form.project.id 까지 접근하므로 N+1 방지를 위해 fetch join 이 필요하다.
     * 임시저장(DRAFT)은 항상 제외된다.
     * <p>
     * 정렬 (DB 단 baseline): matchingRound.phase ASC -> projectApplication.submittedAt ASC. 최종 화면 정렬(phase -> part ->
     * submittedAt)은 part 가 cross-domain 정보라 Assembler 에서 in-memory 로 마무리한다.
     * <p>
     * 기본적으로 지원(모집)이 끝난 차수({@code endsAt < now})의 지원서만 노출한다. matchingRoundId 미지정(전체 조회) 시에도 진행 중인 차수의
     * 지원서는 자동으로 제외된다. {@code includeOngoingMatchingRounds} 가 true 면 이 시간 조건을 적용하지 않는다.
     */
    public List<ProjectApplication> searchProjectApplications(
        Long projectId,
        Long matchingRoundId,
        ProjectApplicationStatus status,
        Instant now,
        boolean includeOngoingMatchingRounds
    ) {
        return queryFactory
            .selectFrom(projectApplication)
            .innerJoin(projectApplication.applicationForm, projectApplicationForm).fetchJoin()
            .innerJoin(projectApplicationForm.project, project).fetchJoin()
            .innerJoin(projectApplication.appliedMatchingRound, projectMatchingRound).fetchJoin()
            .where(
                projectApplicationForm.project.id.eq(projectId),
                matchingRoundIdEq(matchingRoundId),
                managedStatusCond(status),
                matchingRoundViewableCond(now, includeOngoingMatchingRounds)
            )
            .orderBy(projectMatchingRound.phase.asc(), projectApplication.submittedAt.asc())
            .fetch();
    }

    /**
     * PM/운영진용 복수 프로젝트의 지원자 목록을 조회한다.
     * <p>
     * 단건 조회와 동일하게 DRAFT 를 제외하고, {@code includeOngoingProjectIds} 에 포함된 프로젝트만 진행 중 차수의 지원서를 함께 노출한다.
     */
    public List<ProjectApplication> searchProjectApplicationsByProjectIds(
        Collection<Long> projectIds,
        Collection<Long> includeOngoingProjectIds,
        Long matchingRoundId,
        ProjectApplicationStatus status,
        Instant now
    ) {
        if (projectIds == null || projectIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
            .selectFrom(projectApplication)
            .innerJoin(projectApplication.applicationForm, projectApplicationForm).fetchJoin()
            .innerJoin(projectApplicationForm.project, project).fetchJoin()
            .innerJoin(projectApplication.appliedMatchingRound, projectMatchingRound).fetchJoin()
            .where(
                project.id.in(projectIds),
                matchingRoundIdEq(matchingRoundId),
                managedStatusCond(status),
                matchingRoundViewableByProjectCond(includeOngoingProjectIds, now)
            )
            .orderBy(project.id.asc(), projectMatchingRound.phase.asc(), projectApplication.submittedAt.asc())
            .fetch();
    }

    private BooleanExpression matchingRoundViewableCond(Instant now, boolean includeOngoingMatchingRounds) {
        if (includeOngoingMatchingRounds) {
            return null;
        }
        return projectMatchingRound.endsAt.before(now);
    }

    private BooleanExpression matchingRoundViewableByProjectCond(
        Collection<Long> includeOngoingProjectIds,
        Instant now
    ) {
        if (includeOngoingProjectIds == null || includeOngoingProjectIds.isEmpty()) {
            return projectMatchingRound.endsAt.before(now);
        }
        return project.id.in(includeOngoingProjectIds)
            .or(projectMatchingRound.endsAt.before(now));
    }

    /**
     * 수동 상태 변경 시 최소선발 검증에 필요한 같은 차수/프로젝트의 결정 가능 지원서를 조회한다.
     */
    public List<ProjectApplication> listDecidableByMatchingRoundIdAndProjectId(Long matchingRoundId, Long projectId) {
        return queryFactory
            .selectFrom(projectApplication)
            .innerJoin(projectApplication.applicationForm, projectApplicationForm).fetchJoin()
            .innerJoin(projectApplicationForm.project, project).fetchJoin()
            .innerJoin(projectApplication.appliedMatchingRound, projectMatchingRound).fetchJoin()
            .where(
                projectMatchingRound.id.eq(matchingRoundId),
                project.id.eq(projectId),
                projectApplication.status.in(List.of(
                    ProjectApplicationStatus.SUBMITTED,
                    ProjectApplicationStatus.APPROVED,
                    ProjectApplicationStatus.REJECTED
                ))
            )
            .fetch();
    }

    /**
     * 프로젝트/멤버 쌍별 APPROVED 지원서 중 가장 최신 매칭 차수를 반환한다.
     * <p>
     * DB 에서 최신 우선 정렬로 가져온 뒤, Java 에서 각 (projectId, memberId) 의 첫 row 만 남겨 DB 벤더별 window function 차이를 피한다.
     */
    public List<ProjectMemberMatchedRoundInfo> listLatestApprovedMatchedRoundsByProjectIdsAndMemberIds(
        Collection<Long> projectIds,
        Collection<Long> memberIds
    ) {
        if (projectIds == null || projectIds.isEmpty() || memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }

        List<ProjectMemberMatchedRoundInfo> rows = queryFactory
            .select(Projections.constructor(
                ProjectMemberMatchedRoundInfo.class,
                project.id,
                projectApplication.applicantMemberId,
                projectMatchingRound.id,
                projectMatchingRound.type,
                projectMatchingRound.phase
            ))
            .from(projectApplication)
            .innerJoin(projectApplication.applicationForm, projectApplicationForm)
            .innerJoin(projectApplicationForm.project, project)
            .innerJoin(projectApplication.appliedMatchingRound, projectMatchingRound)
            .where(
                project.id.in(projectIds),
                projectApplication.applicantMemberId.in(memberIds),
                projectApplication.status.eq(ProjectApplicationStatus.APPROVED)
            )
            .orderBy(
                project.id.asc(),
                projectApplication.applicantMemberId.asc(),
                projectMatchingRound.startsAt.desc(),
                projectApplication.id.desc()
            )
            .fetch();

        Map<ProjectMemberKey, ProjectMemberMatchedRoundInfo> latestByMember = new LinkedHashMap<>();
        for (ProjectMemberMatchedRoundInfo row : rows) {
            latestByMember.putIfAbsent(new ProjectMemberKey(row.projectId(), row.memberId()), row);
        }
        return new ArrayList<>(latestByMember.values());
    }

    /**
     * 단건 상세 조회 — applicationForm/project, appliedMatchingRound 를 fetch join 한다.
     * <p>
     * formResponse 는 별도 도메인이라 본 쿼리에서 fetch 하지 않는다 — 호출자가 cross-domain UseCase 로 로드한다.
     */
    public Optional<ProjectApplication> findByIdWithDetails(Long applicationId) {
        ProjectApplication result = queryFactory
            .selectFrom(projectApplication)
            .innerJoin(projectApplication.applicationForm, projectApplicationForm).fetchJoin()
            .innerJoin(projectApplicationForm.project, project).fetchJoin()
            .innerJoin(projectApplication.appliedMatchingRound, projectMatchingRound).fetchJoin()
            .where(projectApplication.id.eq(applicationId))
            .fetchOne();
        return Optional.ofNullable(result);
    }

    /**
     * 여러 지원서 상세 조회용 fetch join.
     */
    public List<ProjectApplication> findAllByIdInWithDetails(Collection<Long> applicationIds) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
            .selectFrom(projectApplication)
            .innerJoin(projectApplication.applicationForm, projectApplicationForm).fetchJoin()
            .innerJoin(projectApplicationForm.project, project).fetchJoin()
            .innerJoin(projectApplication.appliedMatchingRound, projectMatchingRound).fetchJoin()
            .where(projectApplication.id.in(applicationIds))
            .orderBy(projectApplication.id.asc())
            .fetch();
    }

    private BooleanExpression matchingRoundIdEq(Long matchingRoundId) {
        return matchingRoundId == null ? null : projectMatchingRound.id.eq(matchingRoundId);
    }

    /**
     * PM/운영진 영역 status 필터.
     * <ul>
     *   <li>null -> SUBMITTED/APPROVED/REJECTED 전체 (DRAFT 제외)</li>
     *   <li>명시 -> 해당 상태 단일</li>
     * </ul>
     * Query 단에서 DRAFT 는 사전 차단되므로 (도메인 invariant), 여기서는 그대로 eq 만 적용한다.
     */
    private BooleanExpression managedStatusCond(ProjectApplicationStatus status) {
        return status == null
            ? projectApplication.status.in(List.of(
            ProjectApplicationStatus.SUBMITTED,
            ProjectApplicationStatus.APPROVED,
            ProjectApplicationStatus.REJECTED))
            : projectApplication.status.eq(status);
    }

    /**
     * status null -> DRAFT(임시저장) 제외, 명시 시 정확히 그 상태.
     */
    private BooleanExpression statusCond(ProjectApplicationStatus status) {
        return status == null
            ? projectApplication.status.ne(ProjectApplicationStatus.DRAFT)
            : projectApplication.status.eq(status);
    }

    private record ProjectMemberKey(Long projectId, Long memberId) {
    }
}
