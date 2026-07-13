package com.umc.product.organization.adapter.out.persistence.umcproduct;

import static com.umc.product.organization.domain.QUmcProductLeadership.umcProductLeadership;
import static com.umc.product.organization.domain.QUmcProductMember.umcProductMember;
import static com.umc.product.organization.domain.QUmcProductMemberActivityPeriod.umcProductMemberActivityPeriod;
import static com.umc.product.organization.domain.QUmcProductPartMembership.umcProductPartMembership;
import static com.umc.product.organization.domain.QUmcProductSquadParticipant.umcProductSquadParticipant;

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
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductMemberSearchCondition;

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
        builder.and(partMembershipMatches(condition));
        builder.and(leadershipMatches(condition));
        builder.and(squadParticipationMatches(condition));
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

    private BooleanExpression partMembershipMatches(UmcProductMemberSearchCondition condition) {
        boolean hasPartFilter = condition.chapterId() != null
            || condition.partId() != null
            || condition.partRole() != null
            || condition.position() != null;
        if (!hasPartFilter) {
            return null;
        }

        BooleanBuilder where = new BooleanBuilder()
            .and(umcProductPartMembership.memberActivityPeriod.umcProductMember.eq(umcProductMember));
        where.and(condition.chapterId() == null
            ? null
            : umcProductPartMembership.part.chapter.id.eq(condition.chapterId()));
        where.and(condition.partId() == null
            ? null
            : umcProductPartMembership.part.id.eq(condition.partId()));
        where.and(condition.partRole() == null
            ? null
            : umcProductPartMembership.role.eq(condition.partRole()));
        where.and(condition.position() == null
            ? null
            : umcProductPartMembership.position.eq(condition.position()));
        if (condition.activeOn() != null) {
            where.and(partMembershipActiveOn(condition.activeOn()));
            where.and(partMembershipActivityPeriodActiveOn(condition.activeOn()));
        }

        return JPAExpressions
            .selectOne()
            .from(umcProductPartMembership)
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

    private BooleanExpression squadParticipationMatches(UmcProductMemberSearchCondition condition) {
        if (condition.squadId() == null) {
            return null;
        }

        BooleanBuilder where = new BooleanBuilder()
            .and(umcProductSquadParticipant.memberActivityPeriod.umcProductMember.eq(umcProductMember))
            .and(umcProductSquadParticipant.squad.id.eq(condition.squadId()));
        if (condition.activeOn() != null) {
            where.and(squadParticipationActiveOn(condition.activeOn()));
            where.and(squadParticipationActivityPeriodActiveOn(condition.activeOn()));
        }

        return JPAExpressions
            .selectOne()
            .from(umcProductSquadParticipant)
            .where(where)
            .exists();
    }

    private BooleanExpression activityPeriodActiveOn(LocalDate activeOn) {
        return umcProductMemberActivityPeriod.period.startDate.loe(activeOn)
            .and(umcProductMemberActivityPeriod.period.endDate.isNull()
                .or(umcProductMemberActivityPeriod.period.endDate.goe(activeOn)));
    }

    private BooleanExpression partMembershipActiveOn(LocalDate activeOn) {
        return umcProductPartMembership.period.startDate.loe(activeOn)
            .and(umcProductPartMembership.period.endDate.isNull()
                .or(umcProductPartMembership.period.endDate.goe(activeOn)));
    }

    private BooleanExpression partMembershipActivityPeriodActiveOn(LocalDate activeOn) {
        return umcProductPartMembership.memberActivityPeriod.period.startDate.loe(activeOn)
            .and(umcProductPartMembership.memberActivityPeriod.period.endDate.isNull()
                .or(umcProductPartMembership.memberActivityPeriod.period.endDate.goe(activeOn)));
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

    private BooleanExpression squadParticipationActiveOn(LocalDate activeOn) {
        return umcProductSquadParticipant.period.startDate.loe(activeOn)
            .and(umcProductSquadParticipant.period.endDate.isNull()
                .or(umcProductSquadParticipant.period.endDate.goe(activeOn)));
    }

    private BooleanExpression squadParticipationActivityPeriodActiveOn(LocalDate activeOn) {
        return umcProductSquadParticipant.memberActivityPeriod.period.startDate.loe(activeOn)
            .and(umcProductSquadParticipant.memberActivityPeriod.period.endDate.isNull()
                .or(umcProductSquadParticipant.memberActivityPeriod.period.endDate.goe(activeOn)));
    }
}
