package com.umc.product.project.adapter.out.persistence;

import static com.umc.product.project.domain.QProject.project;
import static com.umc.product.project.domain.QProjectMember.projectMember;
import static com.umc.product.project.domain.QProjectPartQuota.projectPartQuota;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.application.port.in.query.dto.SearchProjectQuery;
import com.umc.product.project.domain.Project;
import com.umc.product.project.domain.enums.PartQuotaStatus;
import com.umc.product.project.domain.enums.ProjectMemberStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

import lombok.RequiredArgsConstructor;

/**
 * Project QueryDSL 동적 검색 구현 (PROJECT-001).
 */
@Repository
@RequiredArgsConstructor
public class ProjectQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 동적 조건으로 프로젝트 목록을 페이지 조회합니다.
     */
    public Page<Project> search(SearchProjectQuery query) {
        BooleanBuilder condition = buildCondition(query);
        Pageable pageable = query.pageable();

        JPQLQuery<Project> contentQuery = queryFactory
            .selectFrom(project)
            .where(condition)
            .orderBy(toOrderSpecifiers(pageable.getSort()));

        // Pageable.unpaged() 는 offset/pageSize 호출 시 UnsupportedOperationException 을 던지므로
        // 페이징 요청일 때만 offset/limit 을 적용하고, unpaged 면 조건에 맞는 전건을 반환한다.
        if (pageable.isPaged()) {
            contentQuery
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());
        }

        List<Project> content = contentQuery.fetch();

        Long total = queryFactory
            .select(project.count())
            .from(project)
            .where(condition)
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private BooleanBuilder buildCondition(SearchProjectQuery query) {
        BooleanBuilder common = new BooleanBuilder()
            .and(gisuIdEq(query.gisuId()))
            .and(keywordContains(query.keyword()))
            .and(partAndQuotaFilter(query.parts(), query.partQuotaStatus()));

        BooleanBuilder scoped = new BooleanBuilder()
            .and(chapterIdEq(query.chapterId()))
            .and(productOwnerSchoolIdsIn(query.productOwnerSchoolIds()))
            .and(productOwnerMemberIdEq(query.productOwnerMemberId()))
            .and(statusIn(query.statuses()));

        BooleanExpression includedOwner = includedOwnerCond(query);
        if (includedOwner != null) {
            scoped.or(includedOwner);
        }

        return common.and(scoped);
    }

    private BooleanExpression productOwnerMemberIdEq(Long memberId) {
        return memberId != null ? project.productOwnerMemberId.eq(memberId) : null;
    }

    private BooleanExpression includedOwnerCond(SearchProjectQuery query) {
        return query.includedOwnerMemberId() != null
            ? project.productOwnerMemberId.eq(query.includedOwnerMemberId())
                .and(project.status.in(query.includedOwnerStatuses()))
            : null;
    }

    private BooleanExpression gisuIdEq(Long gisuId) {
        return project.gisuId.eq(gisuId);
    }

    private BooleanExpression keywordContains(String keyword) {
        return (keyword != null && !keyword.isBlank())
            ? project.name.containsIgnoreCase(keyword)
            : null;
    }

    private BooleanExpression chapterIdEq(Long chapterId) {
        return chapterId != null ? project.chapterId.eq(chapterId) : null;
    }

    private BooleanExpression productOwnerSchoolIdsIn(List<Long> schoolIds) {
        return (schoolIds == null || schoolIds.isEmpty())
            ? null
            : project.productOwnerSchoolId.in(schoolIds);
    }

    /**
     * 파트 목록과 모집 상태를 <b>상관(correlated)</b>으로 결합해 필터링합니다.
     * <ul>
     *   <li>{@code parts} 있고 {@code status} 있음 → 선택한 각 파트가 존재하며 해당 파트가 지정 상태 (AND)</li>
     *   <li>{@code parts} 있고 {@code status} 없음 → 선택한 각 파트가 존재 (AND, 상태 무관)</li>
     *   <li>{@code parts} 없고 {@code status} 있음 → 프로젝트 전체 기준 (아무 파트 하나라도 모집중 / 모든 파트 완료)</li>
     *   <li>둘 다 없음 → 필터 미적용</li>
     * </ul>
     */
    private BooleanExpression partAndQuotaFilter(List<ChallengerPart> parts, PartQuotaStatus status) {
        boolean hasParts = parts != null && !parts.isEmpty();
        if (!hasParts && status == null) {
            return null;
        }
        if (!hasParts) {
            return projectWideQuotaStatus(status);
        }
        BooleanExpression combined = null;
        for (ChallengerPart part : parts) {
            BooleanExpression perPart = partExistsWithStatus(part, status);
            combined = (combined == null) ? perPart : combined.and(perPart);
        }
        return combined;
    }

    /**
     * 특정 파트가 프로젝트에 존재하는지 + (지정 시) 해당 파트가 모집중/완료 상태인지 체크.
     */
    private BooleanExpression partExistsWithStatus(ChallengerPart part, PartQuotaStatus status) {
        BooleanBuilder where = new BooleanBuilder()
            .and(projectPartQuota.project.eq(project))
            .and(projectPartQuota.part.eq(part));

        if (status != null) {
            BooleanExpression recruiting = projectPartQuota.quota.gt(activeMemberCountForPart(part));
            where.and(status == PartQuotaStatus.RECRUITING ? recruiting : recruiting.not());
        }

        return JPAExpressions.selectOne().from(projectPartQuota).where(where).exists();
    }

    /**
     * 프로젝트 전체 기준 모집 상태.
     * RECRUITING = 모집중 파트가 하나라도 있음, COMPLETED = 파트가 존재하면서 모두 정원 이상.
     */
    private BooleanExpression projectWideQuotaStatus(PartQuotaStatus status) {
        BooleanExpression hasAnyPart = JPAExpressions
            .selectOne()
            .from(projectPartQuota)
            .where(projectPartQuota.project.eq(project))
            .exists();

        BooleanExpression hasRecruitingPart = JPAExpressions
            .selectOne()
            .from(projectPartQuota)
            .where(
                projectPartQuota.project.eq(project),
                projectPartQuota.quota.gt(
                    JPAExpressions
                        .select(projectMember.count())
                        .from(projectMember)
                        .where(
                            projectMember.project.eq(project),
                            projectMember.part.eq(projectPartQuota.part),
                            projectMember.status.eq(ProjectMemberStatus.ACTIVE)
                        )
                )
            )
            .exists();

        return status == PartQuotaStatus.RECRUITING
            ? hasRecruitingPart
            : hasAnyPart.and(hasRecruitingPart.not());
    }

    private JPQLQuery<Long> activeMemberCountForPart(ChallengerPart part) {
        return JPAExpressions
            .select(projectMember.count())
            .from(projectMember)
            .where(
                projectMember.project.eq(project),
                projectMember.part.eq(part),
                projectMember.status.eq(ProjectMemberStatus.ACTIVE)
            );
    }

    private BooleanExpression statusIn(List<ProjectStatus> statuses) {
        return project.status.in(statuses);
    }

    /**
     * Pageable의 Sort를 QueryDSL OrderSpecifier 배열로 변환합니다.
     * 정렬 조건이 없으면 createdAt 오름차순, name 오름차순을 기본으로 사용합니다.
     */
    private OrderSpecifier<?>[] toOrderSpecifiers(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return new OrderSpecifier<?>[]{project.createdAt.asc(), project.name.asc()};
        }
        PathBuilder<Project> path = new PathBuilder<>(Project.class, project.getMetadata());
        List<OrderSpecifier<?>> specifiers = new ArrayList<>();
        for (Sort.Order order : sort) {
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            specifiers.add(new OrderSpecifier<>(direction, path.getComparable(order.getProperty(), Comparable.class)));
        }
        return specifiers.toArray(new OrderSpecifier<?>[0]);
    }
}
