package com.umc.product.inquiry.adapter.out.persistence;

import static com.umc.product.inquiry.domain.QInquiry.inquiry;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.inquiry.application.access.InquiryAccessScope;
import com.umc.product.inquiry.application.access.InquiryAccessScope.OperatorScoped;
import com.umc.product.inquiry.application.access.InquiryAccessScope.OwnedOnly;
import com.umc.product.inquiry.application.access.InquiryAccessScope.TargetCondition;
import com.umc.product.inquiry.application.port.in.query.dto.GetInquiryListQuery;
import com.umc.product.inquiry.domain.Inquiry;
import com.umc.product.inquiry.domain.enums.InquiryTarget;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class InquiryQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<Inquiry> listByScope(InquiryAccessScope scope, GetInquiryListQuery filter) {
        BooleanBuilder condition = new BooleanBuilder();
        condition.and(toScopeCondition(scope));
        condition.and(statusEq(filter));
        condition.and(targetEq(filter));
        condition.and(categoryEq(filter));
        condition.and(cursorLt(filter.cursorId()));

        return queryFactory
            .selectFrom(inquiry)
            .where(condition)
            .orderBy(inquiry.id.desc())
            .limit((long) filter.size() + 1)
            .fetch();
    }

    // ── scope → BooleanExpression ────────────────────────────────────────────

    private BooleanBuilder toScopeCondition(InquiryAccessScope scope) {
        return switch (scope) {
            case OwnedOnly o -> new BooleanBuilder(inquiry.authorMemberId.eq(o.memberId()));
            case OperatorScoped o -> toOperatorCondition(o);
        };
    }

    private BooleanBuilder toOperatorCondition(OperatorScoped scope) {
        BooleanBuilder builder = new BooleanBuilder();
        for (TargetCondition cond : scope.conditions()) {
            builder.or(toTargetCondition(cond));
        }
        builder.or(inquiry.authorMemberId.eq(scope.memberId()));
        return builder;   // 이제 반환타입이 BooleanBuilder라 OK
    }

    private BooleanExpression toTargetCondition(TargetCondition cond) {
        BooleanExpression base = inquiry.target.eq(cond.target())
            .and(inquiry.targetGisuId.eq(cond.gisuId()));

        if (cond.target() == InquiryTarget.SCHOOL) {
            return base.and(inquiry.targetSchoolId.eq(cond.organizationId()));
        }
        if (cond.target() == InquiryTarget.CHAPTER) {
            return base.and(inquiry.targetChapterId.eq(cond.organizationId()));
        }
        // CENTRAL / PRODUCT_TEAM: organizationId 없음, target + gisuId만
        return base;
    }

    // ── 필터 헬퍼 (null 반환 = 조건 무시) ────────────────────────────────────

    private BooleanExpression statusEq(GetInquiryListQuery filter) {
        return filter.status() != null ? inquiry.status.eq(filter.status()) : null;
    }

    private BooleanExpression targetEq(GetInquiryListQuery filter) {
        return filter.target() != null ? inquiry.target.eq(filter.target()) : null;
    }

    private BooleanExpression categoryEq(GetInquiryListQuery filter) {
        return filter.category() != null ? inquiry.category.eq(filter.category()) : null;
    }

    private BooleanExpression cursorLt(Long cursorId) {
        return cursorId != null ? inquiry.id.lt(cursorId) : null;
    }
}
