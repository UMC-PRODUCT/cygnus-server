package com.umc.product.demoday.adapter.out.persistence;

import static com.umc.product.demoday.domain.QDemodayVote.demodayVote;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.demoday.application.port.out.DemodayVoteSearchCondition;
import com.umc.product.demoday.domain.DemodayVote;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DemodayVoteQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<DemodayVote> search(DemodayVoteSearchCondition condition) {
        if (condition.memberIds() != null && condition.memberIds().isEmpty()) {
            return List.of();
        }

        return queryFactory
            .selectFrom(demodayVote)
            .where(
                demodayVote.pollId.eq(condition.pollId()),
                voteIdLt(condition.cursor()),
                boothIdEq(condition.boothId()),
                memberIdIn(condition.memberIds())
            )
            .orderBy(demodayVote.id.desc())
            .limit(condition.limit())
            .fetch();
    }

    public Set<Long> listMemberIds(Long pollId, Long boothId) {
        return new LinkedHashSet<>(queryFactory
            .select(demodayVote.memberId)
            .distinct()
            .from(demodayVote)
            .where(
                demodayVote.pollId.eq(pollId),
                demodayVote.memberId.isNotNull(),
                boothIdEq(boothId)
            )
            .fetch());
    }

    private BooleanExpression voteIdLt(Long cursor) {
        return cursor == null ? null : demodayVote.id.lt(cursor);
    }

    private BooleanExpression boothIdEq(Long boothId) {
        return boothId == null ? null : demodayVote.targetBoothId.eq(boothId);
    }

    private BooleanExpression memberIdIn(Set<Long> memberIds) {
        return memberIds == null ? null : demodayVote.memberId.in(memberIds);
    }
}
