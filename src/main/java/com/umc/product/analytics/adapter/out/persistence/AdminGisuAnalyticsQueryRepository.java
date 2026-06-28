package com.umc.product.analytics.adapter.out.persistence;

import static com.umc.product.analytics.adapter.out.persistence.AdminAnalyticsQueryExpressions.chapterMatchedOrNoMapping;
import static com.umc.product.analytics.adapter.out.persistence.AdminAnalyticsQueryExpressions.pointScore;

import java.util.List;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.analytics.application.port.in.query.dto.AdminGisuPointsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminGisuSummaryInfo;
import com.umc.product.analytics.domain.AdminAnalyticsScope;
import com.umc.product.challenger.domain.QChallenger;
import com.umc.product.challenger.domain.QChallengerPoint;
import com.umc.product.challenger.domain.enums.PointType;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.member.domain.QMember;
import com.umc.product.organization.domain.QChapter;
import com.umc.product.organization.domain.QChapterSchool;
import com.umc.product.schedule.domain.QSchedule;
import com.umc.product.schedule.domain.QScheduleParticipant;
import com.umc.product.schedule.domain.enums.AttendanceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdminGisuAnalyticsQueryRepository {

    private final JPAQueryFactory queryFactory;

    public AdminGisuSummaryInfo getGisuSummary(AdminAnalyticsScope scope) {
        QChallenger ch = new QChallenger("gisuChallenger");
        QMember chMember = new QMember("gisuChallengerMember");
        QChapterSchool chCs = new QChapterSchool("gisuChallengerCs");
        QChapter chChap = new QChapter("gisuChallengerChapter");

        NumberExpression<Long> challengerCount = ch.id.count();
        NumberExpression<Long> graduatedCount = new CaseBuilder()
            .when(ch.status.eq(ChallengerStatus.GRADUATED)).then(1L)
            .otherwise(0L).sum();
        NumberExpression<Long> dropoutCount = new CaseBuilder()
            .when(ch.status.in(ChallengerStatus.EXPELLED, ChallengerStatus.WITHDRAWN)).then(1L)
            .otherwise(0L).sum();

        Tuple challengerRow = queryFactory
            .select(challengerCount, graduatedCount, dropoutCount)
            .from(ch)
            .join(chMember).on(chMember.id.eq(ch.memberId))
            .leftJoin(chCs).on(chCs.school.id.eq(chMember.schoolId))
            .leftJoin(chChap).on(chChap.id.eq(chCs.chapter.id).and(chChap.gisu.id.eq(scope.gisuId())))
            .where(challengerScopeCondition(scope, ch, chMember, chCs, chChap))
            .fetchOne();

        QScheduleParticipant participant = new QScheduleParticipant("gisuAttParticipant");
        QSchedule schedule = new QSchedule("gisuAttSchedule");
        QMember scheduleAuthor = new QMember("gisuAttAuthor");
        QChapterSchool authorCs = new QChapterSchool("gisuAttAuthorCs");
        QChapter authorChapter = new QChapter("gisuAttAuthorChapter");

        NumberExpression<Long> totalParticipantCount = participant.id.count();
        NumberExpression<Long> attendedCount = new CaseBuilder()
            .when(participant.attendance.status.in(
                AttendanceStatus.PRESENT, AttendanceStatus.LATE, AttendanceStatus.EXCUSED))
            .then(1L).otherwise(0L).sum();

        Tuple attendanceRow = queryFactory
            .select(totalParticipantCount, attendedCount)
            .from(participant)
            .join(participant.schedule, schedule)
            .join(scheduleAuthor).on(scheduleAuthor.id.eq(schedule.authorMemberId))
            .leftJoin(authorCs).on(authorCs.school.id.eq(scheduleAuthor.schoolId))
            .leftJoin(authorChapter).on(authorChapter.id.eq(authorCs.chapter.id)
                .and(authorChapter.gisu.id.eq(scope.gisuId())))
            .where(scheduleAuthorScopeCondition(scope, scheduleAuthor, authorChapter)
                .and(chapterMatchedOrNoMapping(authorCs, authorChapter))
                .and(schedule.policy.attendanceGraceMinutes.isNotNull()))
            .fetchOne();

        QChallengerPoint point = new QChallengerPoint("gisuPoint");
        QChallenger pointChallenger = new QChallenger("gisuPointChallenger");
        QMember pointMember = new QMember("gisuPointMember");
        QChapterSchool pointCs = new QChapterSchool("gisuPointCs");
        QChapter pointChapter = new QChapter("gisuPointChapter");

        NumberExpression<Double> score = pointScore(point);
        NumberExpression<Double> bonusTotal = new CaseBuilder()
            .when(score.gt(0.0)).then(score).otherwise(0.0).sum();
        NumberExpression<Double> penaltyTotal = new CaseBuilder()
            .when(score.lt(0.0)).then(score.negate()).otherwise(0.0).sum();
        NumberExpression<Long> outCount = new CaseBuilder()
            .when(point.type.eq(PointType.OUT)).then(1L).otherwise(0L).sum();

        Tuple pointsRow = queryFactory
            .select(bonusTotal, penaltyTotal, outCount)
            .from(point)
            .join(point.challenger, pointChallenger)
            .join(pointMember).on(pointMember.id.eq(pointChallenger.memberId))
            .leftJoin(pointCs).on(pointCs.school.id.eq(pointMember.schoolId))
            .leftJoin(pointChapter).on(pointChapter.id.eq(pointCs.chapter.id)
                .and(pointChapter.gisu.id.eq(scope.gisuId())))
            .where(challengerScopeCondition(scope, pointChallenger, pointMember, pointCs, pointChapter))
            .fetchOne();

        return AdminGisuSummaryInfo.of(
            defaultLong(challengerRow, challengerCount),
            defaultLong(challengerRow, graduatedCount),
            defaultLong(challengerRow, dropoutCount),
            defaultLong(attendanceRow, totalParticipantCount),
            defaultLong(attendanceRow, attendedCount),
            defaultDouble(pointsRow, bonusTotal),
            defaultDouble(pointsRow, penaltyTotal),
            defaultLong(pointsRow, outCount)
        );
    }

    public AdminGisuPointsInfo getGisuPoints(AdminAnalyticsScope scope) {
        QChallengerPoint point = new QChallengerPoint("gisuPointsPoint");
        QChallenger ch = new QChallenger("gisuPointsCh");
        QMember member = new QMember("gisuPointsMember");
        QChapterSchool chapterSchool = new QChapterSchool("gisuPointsCs");
        QChapter chapter = new QChapter("gisuPointsChapter");

        NumberExpression<Double> score = pointScore(point);
        NumberExpression<Double> bonusTotal = new CaseBuilder()
            .when(score.gt(0.0)).then(score).otherwise(0.0).sum();
        NumberExpression<Double> penaltyTotal = new CaseBuilder()
            .when(score.lt(0.0)).then(score.negate()).otherwise(0.0).sum();

        List<AdminGisuPointsInfo.ChapterPointsInfo> chapters = queryFactory
            .select(
                chapter.id,
                chapter.name,
                ch.id.countDistinct(),
                bonusTotal,
                penaltyTotal
            )
            .from(point)
            .join(point.challenger, ch)
            .join(member).on(member.id.eq(ch.memberId))
            .leftJoin(chapterSchool).on(chapterSchool.school.id.eq(member.schoolId))
            .leftJoin(chapter).on(chapter.id.eq(chapterSchool.chapter.id)
                .and(chapter.gisu.id.eq(scope.gisuId())))
            .where(challengerScopeCondition(scope, ch, member, chapterSchool, chapter)
                .and(chapter.id.isNotNull()))
            .groupBy(chapter.id, chapter.name)
            .orderBy(chapter.name.asc())
            .fetch()
            .stream()
            .map(row -> AdminGisuPointsInfo.ChapterPointsInfo.of(
                row.get(chapter.id),
                row.get(chapter.name),
                row.get(ch.id.countDistinct()),
                row.get(bonusTotal) != null ? row.get(bonusTotal) : 0.0,
                row.get(penaltyTotal) != null ? row.get(penaltyTotal) : 0.0
            ))
            .toList();

        return AdminGisuPointsInfo.from(chapters);
    }

    private BooleanBuilder challengerScopeCondition(
        AdminAnalyticsScope scope,
        QChallenger ch,
        QMember m,
        QChapterSchool cs,
        QChapter chap
    ) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(ch.gisuId.eq(scope.gisuId()));
        builder.and(chapterMatchedOrNoMapping(cs, chap));
        if (scope.chapterId() != null) {
            builder.and(chap.id.eq(scope.chapterId()));
        }
        if (scope.schoolId() != null) {
            builder.and(m.schoolId.eq(scope.schoolId()));
        }
        if (scope.responsiblePart() != null) {
            builder.and(ch.part.eq(scope.responsiblePart()));
        }
        return builder;
    }

    private BooleanBuilder scheduleAuthorScopeCondition(
        AdminAnalyticsScope scope,
        QMember author,
        QChapter chapter
    ) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(chapter.gisu.id.eq(scope.gisuId()));
        if (scope.chapterId() != null) {
            builder.and(chapter.id.eq(scope.chapterId()));
        }
        if (scope.schoolId() != null) {
            builder.and(author.schoolId.eq(scope.schoolId()));
        }
        return builder;
    }

    private long defaultLong(Tuple row, NumberExpression<Long> expr) {
        if (row == null) return 0L;
        Long value = row.get(expr);
        return value != null ? value : 0L;
    }

    private double defaultDouble(Tuple row, NumberExpression<Double> expr) {
        if (row == null) return 0.0;
        Double value = row.get(expr);
        return value != null ? value : 0.0;
    }
}
