package com.umc.product.demoday.adapter.out.persistence;

import static com.umc.product.demoday.domain.QDemodayBooth.demodayBooth;
import static com.umc.product.demoday.domain.QDemodayStamp.demodayStamp;
import static com.umc.product.demoday.domain.QDemodayVote.demodayVote;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DemodayDashboardQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Map<Long, Long> countActiveVotesByBooth(Long pollId) {
        NumberExpression<Long> voteCount = demodayVote.id.count();

        List<Tuple> rows = queryFactory
            .select(demodayVote.targetBoothId, voteCount)
            .from(demodayVote)
            .where(
                demodayVote.pollId.eq(pollId),
                demodayVote.revokedAt.isNull()
            )
            .groupBy(demodayVote.targetBoothId)
            .fetch();

        return rows.stream()
            .collect(Collectors.toMap(
                row -> row.get(demodayVote.targetBoothId),
                row -> row.get(voteCount)
            ));
    }

    public Map<Long, Long> countActiveStampsByBooth(Long pollId) {
        NumberExpression<Long> stampCount = demodayStamp.id.count();

        List<Tuple> rows = queryFactory
            .select(demodayStamp.boothId, stampCount)
            .from(demodayStamp)
            .join(demodayBooth).on(demodayBooth.id.eq(demodayStamp.boothId))
            .where(
                demodayBooth.pollId.eq(pollId),
                demodayStamp.revokedAt.isNull()
            )
            .groupBy(demodayStamp.boothId)
            .fetch();

        return rows.stream()
            .collect(Collectors.toMap(
                row -> row.get(demodayStamp.boothId),
                row -> row.get(stampCount)
            ));
    }
}
