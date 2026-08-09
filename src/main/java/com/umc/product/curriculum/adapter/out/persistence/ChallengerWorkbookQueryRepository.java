package com.umc.product.curriculum.adapter.out.persistence;

import static com.umc.product.curriculum.domain.QChallengerWorkbook.challengerWorkbook;
import static com.umc.product.curriculum.domain.QOriginalWorkbook.originalWorkbook;
import static com.umc.product.curriculum.domain.QWeeklyCurriculum.weeklyCurriculum;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort.ChallengerWorkbookLookupKey;
import com.umc.product.curriculum.domain.ChallengerWorkbook;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ChallengerWorkbookQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<ChallengerWorkbook> findByLookupKeys(List<ChallengerWorkbookLookupKey> keys) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }

        BooleanBuilder exactKeys = new BooleanBuilder();
        keys.forEach(key -> exactKeys.or(
            challengerWorkbook.memberId.eq(key.memberId())
                .and(challengerWorkbook.studyGroupId.eq(key.studyGroupId()))
                .and(weeklyCurriculum.id.eq(key.weeklyCurriculumId()))
        ));

        return queryFactory.selectFrom(challengerWorkbook)
            .join(challengerWorkbook.originalWorkbook, originalWorkbook).fetchJoin()
            .join(originalWorkbook.weeklyCurriculum, weeklyCurriculum).fetchJoin()
            .where(exactKeys)
            .fetch();
    }
}
