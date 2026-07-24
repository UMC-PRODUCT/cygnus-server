package com.umc.product.curriculum.adapter.out.persistence;

import static com.umc.product.curriculum.domain.QCurriculum.curriculum;
import static com.umc.product.curriculum.domain.QWeeklyBestWorkbook.weeklyBestWorkbook;
import static com.umc.product.curriculum.domain.QWeeklyCurriculum.weeklyCurriculum;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.application.port.in.query.dto.GetBestWorkbooksQuery;
import com.umc.product.curriculum.domain.WeeklyBestWorkbook;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class WeeklyBestWorkbookQueryRepository {

    private static final int MEMBER_ID_CHUNK_SIZE = 500;

    private final JPAQueryFactory queryFactory;

    public Page<WeeklyBestWorkbook> searchBestWorkbooks(GetBestWorkbooksQuery query) {
        if (!hasValues(query.memberIds()) || query.memberIds().size() <= MEMBER_ID_CHUNK_SIZE) {
            return searchSingle(query);
        }

        long offset = (long) query.page() * query.size();
        long fetchLimit = offset > Long.MAX_VALUE - query.size()
            ? Long.MAX_VALUE
            : offset + query.size();
        List<WeeklyBestWorkbook> candidates = new ArrayList<>();
        long total = 0;
        for (Set<Long> memberIdChunk : partitionMemberIds(query.memberIds())) {
            GetBestWorkbooksQuery chunkQuery = query.withMemberIds(memberIdChunk);
            candidates.addAll(fetchContent(chunkQuery, 0, fetchLimit));
            total += count(chunkQuery);
        }
        List<WeeklyBestWorkbook> content = candidates.stream()
            .sorted(Comparator.comparing(WeeklyBestWorkbook::getId, Comparator.reverseOrder()))
            .skip(offset)
            .limit(query.size())
            .toList();
        return page(query, content, total);
    }

    static List<Set<Long>> partitionMemberIds(Set<Long> memberIds) {
        List<Long> values = new ArrayList<>(memberIds);
        List<Set<Long>> chunks = new ArrayList<>(
            (values.size() + MEMBER_ID_CHUNK_SIZE - 1) / MEMBER_ID_CHUNK_SIZE
        );
        for (int start = 0; start < values.size(); start += MEMBER_ID_CHUNK_SIZE) {
            int end = Math.min(start + MEMBER_ID_CHUNK_SIZE, values.size());
            chunks.add(new LinkedHashSet<>(values.subList(start, end)));
        }
        return chunks;
    }

    private Page<WeeklyBestWorkbook> searchSingle(GetBestWorkbooksQuery query) {
        long offset = (long) query.page() * query.size();
        return page(query, fetchContent(query, offset, query.size()), count(query));
    }

    private List<WeeklyBestWorkbook> fetchContent(
        GetBestWorkbooksQuery query,
        long offset,
        long limit
    ) {
        return queryFactory
            .selectFrom(weeklyBestWorkbook)
            .join(weeklyBestWorkbook.weeklyCurriculum, weeklyCurriculum).fetchJoin()
            .join(weeklyCurriculum.curriculum, curriculum).fetchJoin()
            .where(
                gisuIdEq(query.gisuId()),
                memberIdIn(query.memberIds()),
                partIn(query.parts()),
                weekNoIn(query.weekNos()),
                studyGroupIdIn(query.studyGroupIds())
            )
            .orderBy(weeklyBestWorkbook.id.desc())
            .offset(offset)
            .limit(limit)
            .fetch();
    }

    private long count(GetBestWorkbooksQuery query) {
        Long total = queryFactory
            .select(weeklyBestWorkbook.count())
            .from(weeklyBestWorkbook)
            .join(weeklyBestWorkbook.weeklyCurriculum, weeklyCurriculum)
            .join(weeklyCurriculum.curriculum, curriculum)
            .where(
                gisuIdEq(query.gisuId()),
                memberIdIn(query.memberIds()),
                partIn(query.parts()),
                weekNoIn(query.weekNos()),
                studyGroupIdIn(query.studyGroupIds())
            )
            .fetchOne();
        return total == null ? 0 : total;
    }

    private Page<WeeklyBestWorkbook> page(
        GetBestWorkbooksQuery query,
        List<WeeklyBestWorkbook> content,
        long total
    ) {
        return new PageImpl<>(
            content,
            PageRequest.of(query.page(), query.size()),
            total
        );
    }

    private BooleanExpression gisuIdEq(Long gisuId) {
        return gisuId != null ? curriculum.gisuId.eq(gisuId) : null;
    }

    private BooleanExpression memberIdIn(Set<Long> memberIds) {
        return hasValues(memberIds) ? weeklyBestWorkbook.memberId.in(memberIds) : null;
    }

    private BooleanExpression partIn(Set<ChallengerPart> parts) {
        return hasValues(parts) ? curriculum.part.in(parts) : null;
    }

    private BooleanExpression weekNoIn(List<Long> weekNos) {
        return hasValues(weekNos) ? weeklyCurriculum.weekNo.in(weekNos) : null;
    }

    private BooleanExpression studyGroupIdIn(List<Long> studyGroupIds) {
        return hasValues(studyGroupIds) ? weeklyBestWorkbook.studyGroupId.in(studyGroupIds) : null;
    }

    private boolean hasValues(Collection<?> values) {
        return values != null && !values.isEmpty();
    }
}
