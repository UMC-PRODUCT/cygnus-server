package com.umc.product.challenger.adapter.out.persistence;

import static com.umc.product.challenger.domain.QChallengerRecord.challengerRecord;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.challenger.application.port.in.query.dto.ListChallengerRecordsQuery;
import com.umc.product.challenger.domain.ChallengerRecord;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/**
 * ChallengerRecord QueryDSL 동적 검색 구현 (CHALLENGER-RECORD-103).
 */
@Repository
@RequiredArgsConstructor
public class ChallengerRecordQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 기수/학교/파트/역할 동적 조건으로 챌린저 기록 코드 목록을 페이지 조회합니다.
     */
    public Page<ChallengerRecord> search(ListChallengerRecordsQuery query) {
        BooleanBuilder condition = buildCondition(query);
        Pageable pageable = query.pageable();

        JPQLQuery<ChallengerRecord> contentQuery = queryFactory
            .selectFrom(challengerRecord)
            .where(condition)
            .orderBy(toOrderSpecifiers(pageable.getSort()));

        // Pageable.unpaged() 는 offset/pageSize 호출 시 UnsupportedOperationException 을 던지므로
        // 페이징 요청일 때만 offset/limit 을 적용하고, unpaged 면 조건에 맞는 전건을 반환한다.
        if (pageable.isPaged()) {
            contentQuery
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());
        }

        List<ChallengerRecord> content = contentQuery.fetch();

        Long total = queryFactory
            .select(challengerRecord.count())
            .from(challengerRecord)
            .where(condition)
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private BooleanBuilder buildCondition(ListChallengerRecordsQuery query) {
        return new BooleanBuilder()
            .and(gisuIdEq(query.gisuId()))
            .and(schoolIdEq(query.schoolId()))
            .and(partEq(query.part()))
            .and(challengerRoleTypeEq(query.challengerRoleType()));
    }

    private BooleanExpression gisuIdEq(Long gisuId) {
        return gisuId != null ? challengerRecord.gisuId.eq(gisuId) : null;
    }

    private BooleanExpression schoolIdEq(Long schoolId) {
        return schoolId != null ? challengerRecord.schoolId.eq(schoolId) : null;
    }

    private BooleanExpression partEq(ChallengerPart part) {
        return part != null ? challengerRecord.part.eq(part) : null;
    }

    private BooleanExpression challengerRoleTypeEq(ChallengerRoleType challengerRoleType) {
        return challengerRoleType != null ? challengerRecord.challengerRoleType.eq(challengerRoleType) : null;
    }

    /**
     * Pageable의 Sort를 QueryDSL OrderSpecifier 배열로 변환합니다.
     * 정렬 조건이 없으면 createdAt 내림차순, id 내림차순을 기본으로 사용합니다.
     */
    private OrderSpecifier<?>[] toOrderSpecifiers(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return new OrderSpecifier<?>[]{challengerRecord.createdAt.desc(), challengerRecord.id.desc()};
        }
        PathBuilder<ChallengerRecord> path =
            new PathBuilder<>(ChallengerRecord.class, challengerRecord.getMetadata());
        List<OrderSpecifier<?>> specifiers = new ArrayList<>();
        for (Sort.Order order : sort) {
            Order direction = order.isAscending() ? Order.ASC : Order.DESC;
            specifiers.add(new OrderSpecifier<>(direction, path.getComparable(order.getProperty(), Comparable.class)));
        }
        return specifiers.toArray(new OrderSpecifier<?>[0]);
    }
}
