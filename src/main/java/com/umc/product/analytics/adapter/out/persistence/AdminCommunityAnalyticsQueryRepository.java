package com.umc.product.analytics.adapter.out.persistence;

import static com.umc.product.analytics.adapter.out.persistence.AdminAnalyticsQueryExpressions.chapterMatchedOrNoMapping;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsCommunityActivityInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsCommunityActivityInfo.ActivityBucketInfo;
import com.umc.product.analytics.application.port.in.query.dto.Granularity;
import com.umc.product.analytics.domain.AdminAnalyticsScope;
import com.umc.product.challenger.domain.QChallenger;
import com.umc.product.community.adapter.out.persistence.entity.QCommentJpaEntity;
import com.umc.product.community.adapter.out.persistence.entity.QPostJpaEntity;
import com.umc.product.member.domain.QMember;
import com.umc.product.organization.domain.QChapter;
import com.umc.product.organization.domain.QChapterSchool;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AdminCommunityAnalyticsQueryRepository {

    private static final ZoneId DASHBOARD_ZONE = ZoneId.of("Asia/Seoul");

    private final JPAQueryFactory queryFactory;

    public AdminOperationsCommunityActivityInfo getCommunityActivity(
        AdminAnalyticsScope scope,
        Instant from,
        Instant to,
        Granularity granularity
    ) {
        Map<LocalDate, Long> postCountsByBucket = fetchPostCounts(scope, from, to, granularity);
        Map<LocalDate, Long> commentCountsByBucket = fetchCommentCounts(scope, from, to, granularity);

        // 두 맵의 모든 날짜 키를 합산하여 버킷 목록 생성
        TreeMap<LocalDate, long[]> merged = new TreeMap<>();
        postCountsByBucket.forEach((date, count) ->
            merged.computeIfAbsent(date, d -> new long[2])[0] = count
        );
        commentCountsByBucket.forEach((date, count) ->
            merged.computeIfAbsent(date, d -> new long[2])[1] = count
        );

        List<ActivityBucketInfo> buckets = new ArrayList<>();
        long totalPostCount = 0L;
        long totalCommentCount = 0L;

        for (Map.Entry<LocalDate, long[]> entry : merged.entrySet()) {
            long postCount = entry.getValue()[0];
            long commentCount = entry.getValue()[1];
            buckets.add(ActivityBucketInfo.of(entry.getKey(), postCount, commentCount));
            totalPostCount += postCount;
            totalCommentCount += commentCount;
        }

        return AdminOperationsCommunityActivityInfo.of(buckets, totalPostCount, totalCommentCount);
    }

    private Map<LocalDate, Long> fetchPostCounts(
        AdminAnalyticsScope scope,
        Instant from,
        Instant to,
        Granularity granularity
    ) {
        QPostJpaEntity post = new QPostJpaEntity("comPost");
        QChallenger challenger = new QChallenger("comPostChallenger");
        QMember member = new QMember("comPostMember");
        QChapterSchool chapterSchool = new QChapterSchool("comPostChapterSchool");
        QChapter chapter = new QChapter("comPostChapter");

        BooleanBuilder condition = challengerScopeCondition(scope, challenger, member, chapterSchool, chapter);
        if (from != null) {
            condition.and(post.createdAt.goe(from));
        }
        if (to != null) {
            condition.and(post.createdAt.lt(to));
        }

        List<Instant> createdAts = queryFactory
            .select(post.createdAt)
            .from(post)
            .join(challenger).on(challenger.id.eq(post.authorChallengerId))
            .join(member).on(member.id.eq(challenger.memberId))
            .leftJoin(chapterSchool).on(chapterSchool.school.id.eq(member.schoolId))
            .leftJoin(chapter).on(chapter.id.eq(chapterSchool.chapter.id)
                .and(chapter.gisu.id.eq(challenger.gisuId)))
            .where(condition)
            .fetch();

        return createdAts.stream()
            .collect(Collectors.groupingBy(
                instant -> toDateBucket(instant, granularity),
                TreeMap::new,
                Collectors.counting()
            ));
    }

    private Map<LocalDate, Long> fetchCommentCounts(
        AdminAnalyticsScope scope,
        Instant from,
        Instant to,
        Granularity granularity
    ) {
        QCommentJpaEntity comment = new QCommentJpaEntity("comComment");
        QChallenger challenger = new QChallenger("comCommentChallenger");
        QMember member = new QMember("comCommentMember");
        QChapterSchool chapterSchool = new QChapterSchool("comCommentChapterSchool");
        QChapter chapter = new QChapter("comCommentChapter");

        BooleanBuilder condition = challengerScopeCondition(scope, challenger, member, chapterSchool, chapter);
        if (from != null) {
            condition.and(comment.createdAt.goe(from));
        }
        if (to != null) {
            condition.and(comment.createdAt.lt(to));
        }

        List<Instant> createdAts = queryFactory
            .select(comment.createdAt)
            .from(comment)
            .join(challenger).on(challenger.id.eq(comment.challengerId))
            .join(member).on(member.id.eq(challenger.memberId))
            .leftJoin(chapterSchool).on(chapterSchool.school.id.eq(member.schoolId))
            .leftJoin(chapter).on(chapter.id.eq(chapterSchool.chapter.id)
                .and(chapter.gisu.id.eq(challenger.gisuId)))
            .where(condition)
            .fetch();

        return createdAts.stream()
            .collect(Collectors.groupingBy(
                instant -> toDateBucket(instant, granularity),
                TreeMap::new,
                Collectors.counting()
            ));
    }

    private BooleanBuilder challengerScopeCondition(
        AdminAnalyticsScope scope,
        QChallenger challenger,
        QMember member,
        QChapterSchool chapterSchool,
        QChapter chapter
    ) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(challenger.gisuId.eq(scope.gisuId()));
        // chapter_school 의 다중-기수 중복 행 dedup. orphan 학교(매핑 없음)는 보존한다.
        builder.and(chapterMatchedOrNoMapping(chapterSchool, chapter));
        if (scope.chapterId() != null) {
            builder.and(chapter.id.eq(scope.chapterId()));
        }
        if (scope.schoolId() != null) {
            builder.and(member.schoolId.eq(scope.schoolId()));
        }
        return builder;
    }

    private LocalDate toDateBucket(Instant instant, Granularity granularity) {
        LocalDate date = instant.atZone(DASHBOARD_ZONE).toLocalDate();
        return granularity == Granularity.WEEKLY
            ? date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            : date.withDayOfMonth(1);
    }
}
