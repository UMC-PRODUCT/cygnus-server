package com.umc.product.inhouse.adapter.out.persistence;

import static com.umc.product.inhouse.domain.QUmcProductChapterMembership.umcProductChapterMembership;
import static com.umc.product.inhouse.domain.QUmcProductDepartmentParticipant.umcProductDepartmentParticipant;
import static com.umc.product.inhouse.domain.QUmcProductLeadership.umcProductLeadership;
import static com.umc.product.inhouse.domain.QUmcProductMember.umcProductMember;
import static com.umc.product.inhouse.domain.QUmcProductMemberActivityPeriod.umcProductMemberActivityPeriod;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.inhouse.application.port.in.query.dto.UmcProductMemberSearchCondition;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UmcProductMemberQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<Long> searchMemberIds(UmcProductMemberSearchCondition condition, Pageable pageable) {
        BooleanBuilder where = buildCondition(condition);

        List<Long> content = queryFactory
            .select(umcProductMember.id)
            .from(umcProductMember)
            .where(where)
            .orderBy(umcProductMember.id.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = queryFactory
            .select(umcProductMember.count())
            .from(umcProductMember)
            .where(where)
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private BooleanBuilder buildCondition(UmcProductMemberSearchCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();
        if (condition == null) {
            return builder;
        }
        builder.and(memberActivityPeriodActiveOn(condition.activeOn()));
        builder.and(chapterMembershipMatches(condition));
        builder.and(leadershipMatches(condition));
        builder.and(departmentParticipationMatches(condition));
        return builder;
    }

    private BooleanExpression memberActivityPeriodActiveOn(LocalDate activeOn) {
        if (activeOn == null) {
            return null;
        }
        return JPAExpressions
            .selectOne()
            .from(umcProductMemberActivityPeriod)
            .where(
                umcProductMemberActivityPeriod.umcProductMember.eq(umcProductMember),
                activityPeriodActiveOn(activeOn)
            )
            .exists();
    }

    private BooleanExpression chapterMembershipMatches(UmcProductMemberSearchCondition condition) {
        boolean hasChapterFilter = condition.chapterId() != null
            || condition.position() != null;
        if (!hasChapterFilter) {
            return null;
        }

        BooleanBuilder where = new BooleanBuilder()
            .and(umcProductChapterMembership.memberActivityPeriod.umcProductMember.eq(umcProductMember));
        where.and(condition.chapterId() == null
            ? null
            : umcProductChapterMembership.chapter.id.eq(condition.chapterId()));
        where.and(condition.position() == null
            ? null
            : umcProductChapterMembership.position.eq(condition.position()));
        if (condition.activeOn() != null) {
            where.and(chapterMembershipActiveOn(condition.activeOn()));
            where.and(chapterMembershipActivityPeriodActiveOn(condition.activeOn()));
        }

        return JPAExpressions
            .selectOne()
            .from(umcProductChapterMembership)
            .where(where)
            .exists();
    }

    private BooleanExpression leadershipMatches(UmcProductMemberSearchCondition condition) {
        if (condition.leadershipRole() == null) {
            return null;
        }

        BooleanBuilder where = new BooleanBuilder()
            .and(umcProductLeadership.memberActivityPeriod.umcProductMember.eq(umcProductMember))
            .and(umcProductLeadership.role.eq(condition.leadershipRole()));
        if (condition.activeOn() != null) {
            where.and(leadershipActiveOn(condition.activeOn()));
            where.and(leadershipActivityPeriodActiveOn(condition.activeOn()));
        }

        return JPAExpressions
            .selectOne()
            .from(umcProductLeadership)
            .where(where)
            .exists();
    }

    private BooleanExpression departmentParticipationMatches(UmcProductMemberSearchCondition condition) {
        if (condition.departmentId() == null) {
            return null;
        }

        BooleanBuilder where = new BooleanBuilder()
            .and(umcProductDepartmentParticipant.memberActivityPeriod.umcProductMember.eq(umcProductMember))
            .and(umcProductDepartmentParticipant.department.id.eq(condition.departmentId()));
        if (condition.activeOn() != null) {
            where.and(departmentParticipationActiveOn(condition.activeOn()));
            where.and(departmentParticipationActivityPeriodActiveOn(condition.activeOn()));
        }

        return JPAExpressions
            .selectOne()
            .from(umcProductDepartmentParticipant)
            .where(where)
            .exists();
    }

    private BooleanExpression activityPeriodActiveOn(LocalDate activeOn) {
        return umcProductMemberActivityPeriod.period.startDate.loe(activeOn)
            .and(umcProductMemberActivityPeriod.period.endDate.isNull()
                .or(umcProductMemberActivityPeriod.period.endDate.goe(activeOn)));
    }

    private BooleanExpression chapterMembershipActiveOn(LocalDate activeOn) {
        return umcProductChapterMembership.period.startDate.loe(activeOn)
            .and(umcProductChapterMembership.period.endDate.isNull()
                .or(umcProductChapterMembership.period.endDate.goe(activeOn)));
    }

    private BooleanExpression chapterMembershipActivityPeriodActiveOn(LocalDate activeOn) {
        return umcProductChapterMembership.memberActivityPeriod.period.startDate.loe(activeOn)
            .and(umcProductChapterMembership.memberActivityPeriod.period.endDate.isNull()
                .or(umcProductChapterMembership.memberActivityPeriod.period.endDate.goe(activeOn)));
    }

    private BooleanExpression leadershipActiveOn(LocalDate activeOn) {
        return umcProductLeadership.period.startDate.loe(activeOn)
            .and(umcProductLeadership.period.endDate.isNull()
                .or(umcProductLeadership.period.endDate.goe(activeOn)));
    }

    private BooleanExpression leadershipActivityPeriodActiveOn(LocalDate activeOn) {
        return umcProductLeadership.memberActivityPeriod.period.startDate.loe(activeOn)
            .and(umcProductLeadership.memberActivityPeriod.period.endDate.isNull()
                .or(umcProductLeadership.memberActivityPeriod.period.endDate.goe(activeOn)));
    }

    private BooleanExpression departmentParticipationActiveOn(LocalDate activeOn) {
        return umcProductDepartmentParticipant.period.startDate.loe(activeOn)
            .and(umcProductDepartmentParticipant.period.endDate.isNull()
                .or(umcProductDepartmentParticipant.period.endDate.goe(activeOn)));
    }

    private BooleanExpression departmentParticipationActivityPeriodActiveOn(LocalDate activeOn) {
        return umcProductDepartmentParticipant.memberActivityPeriod.period.startDate.loe(activeOn)
            .and(umcProductDepartmentParticipant.memberActivityPeriod.period.endDate.isNull()
                .or(umcProductDepartmentParticipant.memberActivityPeriod.period.endDate.goe(activeOn)));
    }
}
