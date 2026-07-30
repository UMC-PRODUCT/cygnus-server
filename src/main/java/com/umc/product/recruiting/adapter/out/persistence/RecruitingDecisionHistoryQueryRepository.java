package com.umc.product.recruiting.adapter.out.persistence;

import static com.umc.product.recruiting.domain.QRecruitingApplication.recruitingApplication;
import static com.umc.product.recruiting.domain.QRecruitingDecisionHistory.recruitingDecisionHistory;
import static com.umc.product.recruiting.domain.QRecruitingRound.recruitingRound;
import static com.umc.product.recruiting.domain.QRecruitingSeason.recruitingSeason;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.recruiting.application.port.out.dto.RecruitingDecisionHistoryRow;
import com.umc.product.recruiting.application.port.out.dto.RecruitingDecisionHistorySearchCondition;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RecruitingDecisionHistoryQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<RecruitingDecisionHistoryRow> searchRows(
        RecruitingDecisionHistorySearchCondition condition,
        Pageable pageable
    ) {
        BooleanExpression predicate = recruitingSeason.gisuId.eq(condition.gisuId())
            .and(schoolIdIn(condition.schoolIds()))
            .and(trackIn(condition.tracks()))
            .and(decisionStatusIn(condition.decisionStatuses()))
            .and(nameMatches(condition.searchName(), condition.matchedDeciderMemberIds()));

        var contentQuery = queryFactory
            .select(Projections.constructor(
                RecruitingDecisionHistoryRow.class,
                recruitingDecisionHistory.id,
                recruitingApplication.id,
                recruitingSeason.schoolId,
                recruitingApplication.applicantProfile.applicantName,
                recruitingApplication.applicantProfile.applicantEmail,
                recruitingApplication.applicantProfile.firstChoice,
                recruitingApplication.applicantProfile.secondChoice,
                recruitingApplication.acceptedTrack,
                recruitingDecisionHistory.decisionStatus,
                recruitingDecisionHistory.decidedAt,
                recruitingDecisionHistory.decidedByMemberId,
                recruitingDecisionHistory.deciderRoleType
            ))
            .from(recruitingDecisionHistory)
            .innerJoin(recruitingDecisionHistory.application, recruitingApplication)
            .innerJoin(recruitingApplication.round, recruitingRound)
            .innerJoin(recruitingRound.season, recruitingSeason)
            .where(predicate)
            .orderBy(orderSpecifiers(condition));
        if (pageable.isPaged()) {
            contentQuery.offset(pageable.getOffset()).limit(pageable.getPageSize());
        }
        List<RecruitingDecisionHistoryRow> content = contentQuery.fetch();

        Long total = queryFactory
            .select(recruitingDecisionHistory.count())
            .from(recruitingDecisionHistory)
            .innerJoin(recruitingDecisionHistory.application, recruitingApplication)
            .innerJoin(recruitingApplication.round, recruitingRound)
            .innerJoin(recruitingRound.season, recruitingSeason)
            .where(predicate)
            .fetchOne();
        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    public List<Long> listDeciderMemberIds(Long gisuId, Set<Long> schoolIds) {
        return queryFactory
            .select(recruitingDecisionHistory.decidedByMemberId)
            .distinct()
            .from(recruitingDecisionHistory)
            .innerJoin(recruitingDecisionHistory.application, recruitingApplication)
            .innerJoin(recruitingApplication.round, recruitingRound)
            .innerJoin(recruitingRound.season, recruitingSeason)
            .where(
                recruitingSeason.gisuId.eq(gisuId),
                schoolIdIn(schoolIds)
            )
            .fetch();
    }

    /**
     * 담당자별 그룹 정렬은 담당자의 범위 내 최초 판정 시각 순으로 그룹을 배치하고, 그룹 내부는 요청한 정렬 순서를 따릅니다.
     * 페이징과 양립해야 하므로 window 함수로 SQL 정렬 단계에서 처리합니다.
     */
    private OrderSpecifier<?>[] orderSpecifiers(RecruitingDecisionHistorySearchCondition condition) {
        DateTimePath<Instant> decidedAt = recruitingDecisionHistory.decidedAt;
        OrderSpecifier<?> decidedAtOrder = condition.latestFirst() ? decidedAt.desc() : decidedAt.asc();
        OrderSpecifier<?> idOrder = condition.latestFirst()
            ? recruitingDecisionHistory.id.desc()
            : recruitingDecisionHistory.id.asc();
        if (!condition.groupByDecider()) {
            return new OrderSpecifier<?>[] {decidedAtOrder, idOrder};
        }
        var firstDecidedAtByDecider = Expressions.dateTimeTemplate(
            Instant.class,
            "min({0}) over (partition by {1})",
            decidedAt,
            recruitingDecisionHistory.decidedByMemberId
        );
        return new OrderSpecifier<?>[] {
            firstDecidedAtByDecider.asc(),
            recruitingDecisionHistory.decidedByMemberId.asc(),
            decidedAtOrder,
            idOrder
        };
    }

    private BooleanExpression schoolIdIn(Set<Long> schoolIds) {
        return schoolIds == null || schoolIds.isEmpty() ? null : recruitingSeason.schoolId.in(schoolIds);
    }

    private BooleanExpression trackIn(Set<ChallengerTrack> tracks) {
        return tracks == null || tracks.isEmpty() ? null
            : recruitingApplication.applicantProfile.firstChoice.in(tracks)
                .or(recruitingApplication.applicantProfile.secondChoice.in(tracks));
    }

    private BooleanExpression decisionStatusIn(Set<RecruitingApplicationStatus> statuses) {
        return statuses == null || statuses.isEmpty() ? null
            : recruitingDecisionHistory.decisionStatus.in(statuses);
    }

    private BooleanExpression nameMatches(String searchName, Set<Long> matchedDeciderMemberIds) {
        if (searchName == null) {
            return null;
        }
        BooleanExpression applicantNameMatches = recruitingApplication.applicantProfile.applicantName
            .lower()
            .contains(searchName.toLowerCase(Locale.ROOT));
        if (matchedDeciderMemberIds == null || matchedDeciderMemberIds.isEmpty()) {
            return applicantNameMatches;
        }
        return applicantNameMatches.or(recruitingDecisionHistory.decidedByMemberId.in(matchedDeciderMemberIds));
    }
}
