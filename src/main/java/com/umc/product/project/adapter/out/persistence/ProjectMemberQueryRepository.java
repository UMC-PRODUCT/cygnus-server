package com.umc.product.project.adapter.out.persistence;

import static com.umc.product.project.domain.QProjectMember.projectMember;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Repository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.domain.ProjectMember;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.ProjectMemberStatus;

import lombok.RequiredArgsConstructor;

/**
 * ProjectMember QueryDSL 기반 조회 구현.
 */
@Repository
@RequiredArgsConstructor
public class ProjectMemberQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 매칭 종류로부터 노출 가능 파트 집합을 결정한다. 동일 멤버의 다른 파트(예: 같은 사람이 다른 기수에 PLAN 으로 합류한 잔여 row 가 잘못 매칭되는 경우) 를 차단하는 안전망 역할.
     */
    private static Set<ChallengerPart> partsOf(MatchingType matchingType) {
        return switch (matchingType) {
            case PLAN_DESIGN -> EnumSet.of(ChallengerPart.DESIGN);
            case PLAN_DEVELOPER -> EnumSet.of(
                ChallengerPart.WEB,
                ChallengerPart.ANDROID,
                ChallengerPart.IOS,
                ChallengerPart.NODEJS,
                ChallengerPart.SPRINGBOOT
            );
        };
    }

    /**
     * 본인이 application=null + ACTIVE 인 ProjectMember 단건을 매칭 종류 기준 part 필터로 조회한다. APPLY-004 의 랜덤 매칭 카드 합성에 사용된다.
     * <p>
     * project 는 fetch join 으로 함께 로드된다. 도메인 정책상 결과는 0 또는 1 건이며, 2건 이상이면 QueryDSL {@code fetchOne()} 이
     * {@code NonUniqueResultException} 을 던져 데이터 정합성 위반이 표면화된다.
     */
    public Optional<ProjectMember> findActiveWithoutApplicationByMemberIdAndGisuIdAndMatchingType(
        Long memberId, Long gisuId, MatchingType matchingType
    ) {
        Set<ChallengerPart> partFilter = partsOf(matchingType);

        ProjectMember result = queryFactory
            .selectFrom(projectMember)
            .join(projectMember.project).fetchJoin()
            .where(
                projectMember.memberId.eq(memberId),
                projectMember.project.gisuId.eq(gisuId),
                projectMember.application.isNull(),
                projectMember.status.eq(ProjectMemberStatus.ACTIVE),
                projectMember.part.in(partFilter)
            )
            .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * 여러 프로젝트의 특정 파트 ACTIVE 멤버를 한 번에 조회해 projectId 기준으로 그룹핑한다.
     * <p>
     * {@code projectMember.project.id} projection 으로 FK 만 추출하여, 호출자가 {@code member.getProject().getId()} 로 lazy
     * 프록시에 접근하지 않도록 한다.
     *
     * @return projectId -> 멤버 목록 맵. 해당 파트 멤버가 없는 프로젝트는 엔트리 없음.
     */
    public Map<Long, List<ProjectMember>> listByProjectIdsAndPartGroupedByProjectId(
        Collection<Long> projectIds, ChallengerPart part
    ) {
        if (projectIds == null || projectIds.isEmpty()) {
            return Map.of();
        }

        List<Tuple> rows = queryFactory
            .select(projectMember.project.id, projectMember)
            .from(projectMember)
            .where(
                projectMember.project.id.in(projectIds),
                projectMember.part.eq(part),
                projectMember.status.eq(ProjectMemberStatus.ACTIVE)
            )
            .orderBy(projectMember.createdAt.asc(), projectMember.id.asc())
            .fetch();

        Map<Long, List<ProjectMember>> result = new HashMap<>();
        for (Tuple row : rows) {
            Long projectId = row.get(projectMember.project.id);
            ProjectMember member = row.get(projectMember);
            result.computeIfAbsent(projectId, k -> new ArrayList<>()).add(member);
        }
        return result;
    }

    /**
     * 여러 프로젝트 중 특정 멤버가 ACTIVE PLAN 멤버인 프로젝트 ID 를 조회한다.
     */
    public List<Long> listProjectIdsByActivePlanMember(Collection<Long> projectIds, Long memberId) {
        if (projectIds == null || projectIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
            .select(projectMember.project.id)
            .distinct()
            .from(projectMember)
            .where(
                projectMember.project.id.in(projectIds),
                projectMember.memberId.eq(memberId),
                projectMember.part.eq(ChallengerPart.PLAN),
                projectMember.status.eq(ProjectMemberStatus.ACTIVE)
            )
            .fetch();
    }

    /**
     * 여러 프로젝트의 (projectId, part) 별 ACTIVE 멤버 수를 한 번에 집계한다.
     *
     * @return projectId -> (part -> 인원수) 맵. 인원이 0 인 (project, part) 조합은 엔트리 없음.
     */
    public Map<Long, Map<ChallengerPart, Long>> countByProjectIdsGroupByProjectIdAndPart(
        Collection<Long> projectIds
    ) {
        if (projectIds == null || projectIds.isEmpty()) {
            return Map.of();
        }

        List<Tuple> rows = queryFactory
            .select(projectMember.project.id, projectMember.part, projectMember.count())
            .from(projectMember)
            .where(
                projectMember.project.id.in(projectIds),
                projectMember.status.eq(ProjectMemberStatus.ACTIVE)
            )
            .groupBy(projectMember.project.id, projectMember.part)
            .fetch();

        Map<Long, Map<ChallengerPart, Long>> result = new HashMap<>();
        for (Tuple row : rows) {
            Long projectId = row.get(projectMember.project.id);
            ChallengerPart part = row.get(projectMember.part);
            Long count = row.get(projectMember.count());
            result
                .computeIfAbsent(projectId, k -> new EnumMap<>(ChallengerPart.class))
                .put(part, count);
        }
        return result;
    }

}
