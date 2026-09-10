package com.umc.product.analytics.adapter.out.persistence;

import static com.umc.product.analytics.adapter.out.persistence.AdminAnalyticsQueryExpressions.chapterMatchedOrNoMapping;
import static com.umc.product.analytics.adapter.out.persistence.AdminAnalyticsQueryExpressions.pointScore;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceChaptersInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendancePartsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceTagsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsOverviewInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsOverviewQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsPointsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsSchoolsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsSignupsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsStudyGroupsInfo;
import com.umc.product.analytics.domain.AdminAnalyticsScope;
import com.umc.product.challenger.domain.QChallenger;
import com.umc.product.challenger.domain.QChallengerPoint;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.member.domain.QMember;
import com.umc.product.organization.domain.QChapter;
import com.umc.product.organization.domain.QChapterSchool;
import com.umc.product.organization.domain.QSchool;
import com.umc.product.organization.domain.QStudyGroup;
import com.umc.product.organization.domain.QStudyGroupMember;
import com.umc.product.organization.domain.QStudyGroupSchedule;
import com.umc.product.schedule.domain.QSchedule;
import com.umc.product.schedule.domain.QScheduleParticipant;
import com.umc.product.schedule.domain.enums.AttendanceStatus;
import com.umc.product.schedule.domain.enums.ScheduleTag;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AdminOperationsAnalyticsQueryRepository {

    private static final ZoneId DASHBOARD_ZONE = ZoneId.of("Asia/Seoul");

    private final JPAQueryFactory queryFactory;

    public AdminOperationsStudyGroupsInfo getOperationsStudyGroups(AdminAnalyticsScope scope, Instant from, Instant to) {
        QStudyGroup studyGroup = new QStudyGroup("operationsStudyGroup");
        JPAQuery<Long> studyGroupCountQuery = queryFactory
            .select(studyGroup.id.countDistinct())
            .from(studyGroup);
        long studyGroupCount = defaultLong(studyGroupCountQuery
            .where(studyGroupCondition(scope, studyGroup, studyGroupCountQuery))
            .fetchOne());

        QStudyGroupSchedule studyGroupSchedule = new QStudyGroupSchedule("operationsStudyGroupSchedule");
        QStudyGroup scheduledStudyGroup = new QStudyGroup("operationsScheduledStudyGroup");
        JPAQuery<Long> scheduleCountQuery = queryFactory
            .select(studyGroupSchedule.id.countDistinct())
            .from(studyGroupSchedule)
            .join(scheduledStudyGroup).on(scheduledStudyGroup.id.eq(studyGroupSchedule.studyGroupId));
        long studyGroupScheduleCount = defaultLong(scheduleCountQuery
            .where(studyGroupCondition(scope, scheduledStudyGroup, scheduleCountQuery)
                .and(periodCondition(studyGroupSchedule.createdAt, from, to)))
            .fetchOne());

        return AdminOperationsStudyGroupsInfo.of(studyGroupCount, studyGroupScheduleCount);
    }

    public AdminOperationsSignupsInfo getOperationsSignups(AdminAnalyticsScope scope, Instant from, Instant to) {
        QChallenger challenger = new QChallenger("operationsSignupChallenger");
        QMember member = new QMember("operationsSignupMember");
        QChapterSchool chapterSchool = new QChapterSchool("operationsSignupChapterSchool");
        QChapter chapter = new QChapter("operationsSignupChapter");

        List<Tuple> rows = queryFactory
            .select(member.id, member.createdAt)
            .from(challenger)
            .join(member).on(member.id.eq(challenger.memberId))
            .leftJoin(chapterSchool).on(chapterSchool.school.id.eq(member.schoolId))
            .leftJoin(chapter).on(chapter.id.eq(chapterSchool.chapter.id)
                .and(chapter.gisu.id.eq(challenger.gisuId)))
            .where(challengerScopeCondition(scope, challenger, member, chapterSchool, chapter)
                .and(periodCondition(member.createdAt, from, to)))
            .groupBy(member.id, member.createdAt)
            .fetch();

        Map<LocalDate, Long> countsByDate = rows.stream()
            .map(row -> row.get(member.createdAt))
            .filter(createdAt -> createdAt != null)
            .collect(Collectors.groupingBy(
                createdAt -> createdAt.atZone(DASHBOARD_ZONE).toLocalDate(),
                TreeMap::new,
                Collectors.counting()
            ));

        return AdminOperationsSignupsInfo.from(
            countsByDate.entrySet().stream()
                .map(entry -> AdminOperationsSignupsInfo.SignupBucketInfo.of(entry.getKey(), entry.getValue()))
                .toList()
        );
    }

    public AdminOperationsAttendanceInfo getOperationsAttendance(AdminAnalyticsScope scope, Instant from, Instant to) {
        QSchedule schedule = new QSchedule("operationsAttSchedule");
        QMember author = new QMember("operationsAttAuthor");
        QChapterSchool chapterSchool = new QChapterSchool("operationsAttChapterSchool");
        QChapter chapter = new QChapter("operationsAttChapter");

        BooleanBuilder scheduleCondition = scheduleScopeCondition(scope, from, to, schedule, author, chapter);
        Long scheduleCount = queryFactory
            .select(schedule.id.countDistinct())
            .from(schedule)
            .join(author).on(author.id.eq(schedule.authorMemberId))
            .leftJoin(chapterSchool).on(chapterSchool.school.id.eq(author.schoolId))
            .leftJoin(chapter).on(chapter.id.eq(chapterSchool.chapter.id))
            .where(scheduleCondition)
            .fetchOne();

        Long attendanceRequiredScheduleCount = queryFactory
            .select(schedule.id.countDistinct())
            .from(schedule)
            .join(author).on(author.id.eq(schedule.authorMemberId))
            .leftJoin(chapterSchool).on(chapterSchool.school.id.eq(author.schoolId))
            .leftJoin(chapter).on(chapter.id.eq(chapterSchool.chapter.id))
            .where(scheduleScopeCondition(scope, from, to, schedule, author, chapter)
                .and(schedule.policy.attendanceGraceMinutes.isNotNull()))
            .fetchOne();

        QScheduleParticipant participant = new QScheduleParticipant("operationsAttParticipant");
        QSchedule attSchedule = new QSchedule("operationsAttAttendanceSchedule");
        QMember attAuthor = new QMember("operationsAttAttendanceAuthor");
        QChapterSchool attChapterSchool = new QChapterSchool("operationsAttAttChapterSchool");
        QChapter attChapter = new QChapter("operationsAttAttChapter");
        NumberExpression<Long> statusCount = participant.id.count();

        List<Tuple> rows = queryFactory
            .select(participant.attendance.status, statusCount)
            .from(participant)
            .join(participant.schedule, attSchedule)
            .join(attAuthor).on(attAuthor.id.eq(attSchedule.authorMemberId))
            .leftJoin(attChapterSchool).on(attChapterSchool.school.id.eq(attAuthor.schoolId))
            .leftJoin(attChapter).on(attChapter.id.eq(attChapterSchool.chapter.id))
            .where(scheduleAuthorScopeCondition(scope, attAuthor, attChapter)
                .and(periodCondition(participant.updatedAt, from, to))
                .and(participant.attendance.status.isNotNull()))
            .groupBy(participant.attendance.status)
            .fetch();

        Map<AttendanceStatus, Long> attendanceStatusCounts = new EnumMap<>(AttendanceStatus.class);
        for (Tuple row : rows) {
            attendanceStatusCounts.put(row.get(participant.attendance.status), defaultLong(row.get(statusCount)));
        }
        long attendanceRecordCount = attendanceStatusCounts.values().stream().mapToLong(Long::longValue).sum();

        return AdminOperationsAttendanceInfo.of(
            defaultLong(scheduleCount),
            defaultLong(attendanceRequiredScheduleCount),
            attendanceRecordCount,
            attendanceStatusCounts
        );
    }

    public AdminOperationsPointsInfo getOperationsPoints(AdminAnalyticsScope scope, Instant from, Instant to) {
        QChallengerPoint point = new QChallengerPoint("operationsPoint");
        QChallenger challenger = new QChallenger("operationsPointChallenger");
        QMember member = new QMember("operationsPointMember");
        QChapterSchool chapterSchool = new QChapterSchool("operationsPointChapterSchool");
        QChapter chapter = new QChapter("operationsPointChapter");
        NumberExpression<Long> grantCount = point.id.count();
        NumberExpression<Double> pointSum = pointScore(point).sum().coalesce(0.0);

        List<com.querydsl.core.Tuple> rows = queryFactory
            .select(chapter.id, chapter.name, challenger.part, grantCount, pointSum)
            .from(point)
            .join(point.challenger, challenger)
            .join(member).on(member.id.eq(challenger.memberId))
            .leftJoin(chapterSchool).on(chapterSchool.school.id.eq(member.schoolId))
            .leftJoin(chapter).on(chapter.id.eq(chapterSchool.chapter.id)
                .and(chapter.gisu.id.eq(challenger.gisuId)))
            .where(challengerScopeCondition(scope, challenger, member, chapterSchool, chapter)
                .and(periodCondition(point.createdAt, from, to))
                .and(chapter.id.isNotNull()))
            .groupBy(chapter.id, chapter.name, challenger.part)
            .orderBy(chapter.name.asc(), challenger.part.asc())
            .fetch();

        return AdminOperationsPointsInfo.from(
            rows.stream()
                .map(row -> AdminOperationsPointsInfo.ChapterPartPointGrantStatusInfo.of(
                    row.get(chapter.id),
                    row.get(chapter.name),
                    row.get(challenger.part),
                    defaultLong(row.get(grantCount)),
                    defaultDouble(row.get(pointSum))
                ))
                .toList()
        );
    }

    public AdminOperationsSchoolsInfo getOperationsSchools(AdminAnalyticsScope scope) {
        return AdminOperationsSchoolsInfo.from(
            listChapterSchoolStatuses(scope).stream()
                .map(s -> AdminOperationsSchoolsInfo.ChapterStatusInfo.of(
                    s.chapterId(),
                    s.chapterName(),
                    s.schools().stream()
                        .map(school -> AdminOperationsSchoolsInfo.SchoolChallengerStatusInfo.of(
                            school.schoolId(),
                            school.schoolName(),
                            school.totalChallengerCount(),
                            school.challengerPartCounts()
                        ))
                        .toList()
                ))
                .toList()
        );
    }

    public AdminOperationsOverviewInfo getOperationsOverview(
        AdminAnalyticsScope scope,
        AdminOperationsOverviewQuery query
    ) {
        return AdminOperationsOverviewInfo.of(
            listChapterSchoolStatuses(scope),
            listPointGrantStatuses(scope, query),
            getScheduleAttendanceStatus(scope, query),
            getStudyGroupStatus(scope, query),
            listSignupBuckets(scope, query)
        );
    }

    private List<AdminOperationsOverviewInfo.ChapterSchoolStatusInfo> listChapterSchoolStatuses(
        AdminAnalyticsScope scope
    ) {
        QChapterSchool chapterSchool = new QChapterSchool("operationsChapterSchool");
        QChapter chapter = new QChapter("operationsChapter");
        QSchool school = new QSchool("operationsSchool");
        QMember member = new QMember("operationsSchoolMember");
        QChallenger challenger = new QChallenger("operationsSchoolChallenger");
        NumberExpression<Long> challengerCount = challenger.id.countDistinct();

        BooleanBuilder challengerJoinCondition = new BooleanBuilder()
            .and(challenger.memberId.eq(member.id))
            .and(challenger.gisuId.eq(scope.gisuId()));
        if (scope.responsiblePart() != null) {
            challengerJoinCondition.and(challenger.part.eq(scope.responsiblePart()));
        }

        List<Tuple> rows = queryFactory
            .select(chapter.id, chapter.name, school.id, school.name, challenger.part, challengerCount)
            .from(chapterSchool)
            .join(chapterSchool.chapter, chapter)
            .join(chapterSchool.school, school)
            .leftJoin(member).on(member.schoolId.eq(school.id))
            .leftJoin(challenger).on(challengerJoinCondition)
            .where(schoolScopeCondition(scope, chapter, school))
            .groupBy(chapter.id, chapter.name, school.id, school.name, challenger.part)
            .orderBy(chapter.name.asc(), school.name.asc())
            .fetch();

        Map<ChapterKey, Map<SchoolKey, Map<ChallengerPart, Long>>> chapterSchoolPartCounts = new LinkedHashMap<>();
        for (Tuple row : rows) {
            ChapterKey chapterKey = ChapterKey.from(row, chapter);
            SchoolKey schoolKey = SchoolKey.from(row, school);
            ChallengerPart part = row.get(challenger.part);
            long count = defaultLong(row.get(challengerCount));

            Map<SchoolKey, Map<ChallengerPart, Long>> schoolPartCounts =
                chapterSchoolPartCounts.computeIfAbsent(chapterKey, ignored -> new LinkedHashMap<>());
            Map<ChallengerPart, Long> partCounts =
                schoolPartCounts.computeIfAbsent(schoolKey, ignored -> emptyPartCounts());
            if (part != null) {
                partCounts.put(part, count);
            }
        }

        return chapterSchoolPartCounts.entrySet()
            .stream()
            .map(chapterEntry -> AdminOperationsOverviewInfo.ChapterSchoolStatusInfo.of(
                chapterEntry.getKey().chapterId(),
                chapterEntry.getKey().chapterName(),
                chapterEntry.getValue().entrySet()
                    .stream()
                    .map(schoolEntry -> toSchoolChallengerStatus(schoolEntry.getKey(), schoolEntry.getValue()))
                    .toList()
            ))
            .toList();
    }

    private AdminOperationsOverviewInfo.SchoolChallengerStatusInfo toSchoolChallengerStatus(
        SchoolKey school,
        Map<ChallengerPart, Long> partCounts
    ) {
        long total = partCounts.values()
            .stream()
            .mapToLong(Long::longValue)
            .sum();
        return AdminOperationsOverviewInfo.SchoolChallengerStatusInfo.of(
            school.schoolId(),
            school.schoolName(),
            total,
            partCounts
        );
    }

    private List<AdminOperationsOverviewInfo.ChapterPartPointGrantStatusInfo> listPointGrantStatuses(
        AdminAnalyticsScope scope,
        AdminOperationsOverviewQuery query
    ) {
        QChallengerPoint point = new QChallengerPoint("operationsPoint");
        QChallenger challenger = new QChallenger("operationsPointChallenger");
        QMember member = new QMember("operationsPointMember");
        QChapterSchool chapterSchool = new QChapterSchool("operationsPointChapterSchool");
        QChapter chapter = new QChapter("operationsPointChapter");
        NumberExpression<Long> grantCount = point.id.count();
        NumberExpression<Double> pointSum = pointScore(point).sum().coalesce(0.0);

        // chapter 별 그루핑이라 chapter 매핑이 없는 orphan 챌린저의 포인트는 제외한다.
        // 동시에 chapter.id.isNotNull() 가 chapter_school 의 다중-기수 중복 행을 자연스럽게 dedup 한다.
        List<Tuple> rows = queryFactory
            .select(chapter.id, chapter.name, challenger.part, grantCount, pointSum)
            .from(point)
            .join(point.challenger, challenger)
            .join(member).on(member.id.eq(challenger.memberId))
            .leftJoin(chapterSchool).on(chapterSchool.school.id.eq(member.schoolId))
            .leftJoin(chapter).on(chapter.id.eq(chapterSchool.chapter.id)
                .and(chapter.gisu.id.eq(challenger.gisuId)))
            .where(challengerScopeCondition(scope, challenger, member, chapterSchool, chapter)
                .and(periodCondition(point.createdAt, query))
                .and(chapter.id.isNotNull()))
            .groupBy(chapter.id, chapter.name, challenger.part)
            .orderBy(chapter.name.asc(), challenger.part.asc())
            .fetch();

        return rows.stream()
            .map(row -> AdminOperationsOverviewInfo.ChapterPartPointGrantStatusInfo.of(
                row.get(chapter.id),
                row.get(chapter.name),
                row.get(challenger.part),
                defaultLong(row.get(grantCount)),
                defaultDouble(row.get(pointSum))
            ))
            .toList();
    }

    private AdminOperationsOverviewInfo.ScheduleAttendanceStatusInfo getScheduleAttendanceStatus(
        AdminAnalyticsScope scope,
        AdminOperationsOverviewQuery query
    ) {
        QSchedule schedule = new QSchedule("operationsSchedule");
        long scheduleCount = countSchedules(scope, query, schedule, false);
        long attendanceRequiredScheduleCount = countSchedules(scope, query, schedule, true);
        Map<AttendanceStatus, Long> attendanceStatusCounts = countAttendanceStatuses(scope, query);
        long attendanceRecordCount = attendanceStatusCounts.values()
            .stream()
            .mapToLong(Long::longValue)
            .sum();

        return AdminOperationsOverviewInfo.ScheduleAttendanceStatusInfo.of(
            scheduleCount,
            attendanceRequiredScheduleCount,
            attendanceRecordCount,
            attendanceStatusCounts
        );
    }

    private long countSchedules(
        AdminAnalyticsScope scope,
        AdminOperationsOverviewQuery query,
        QSchedule schedule,
        boolean attendanceRequiredOnly
    ) {
        QMember author = new QMember("operationsScheduleAuthor");
        QChapterSchool chapterSchool = new QChapterSchool("operationsScheduleChapterSchool");
        QChapter chapter = new QChapter("operationsScheduleChapter");
        BooleanBuilder condition = scheduleScopeCondition(scope, query, schedule, author, chapter);
        if (attendanceRequiredOnly) {
            condition.and(schedule.policy.attendanceGraceMinutes.isNotNull());
        }

        Long count = queryFactory
            .select(schedule.id.countDistinct())
            .from(schedule)
            .join(author).on(author.id.eq(schedule.authorMemberId))
            .leftJoin(chapterSchool).on(chapterSchool.school.id.eq(author.schoolId))
            .leftJoin(chapter).on(chapter.id.eq(chapterSchool.chapter.id))
            .where(condition)
            .fetchOne();
        return defaultLong(count);
    }

    private Map<AttendanceStatus, Long> countAttendanceStatuses(
        AdminAnalyticsScope scope,
        AdminOperationsOverviewQuery query
    ) {
        QScheduleParticipant participant = new QScheduleParticipant("operationsParticipant");
        QSchedule schedule = new QSchedule("operationsAttendanceSchedule");
        QMember author = new QMember("operationsAttendanceAuthor");
        QChapterSchool chapterSchool = new QChapterSchool("operationsAttendanceChapterSchool");
        QChapter chapter = new QChapter("operationsAttendanceChapter");
        NumberExpression<Long> statusCount = participant.id.count();

        List<Tuple> rows = queryFactory
            .select(participant.attendance.status, statusCount)
            .from(participant)
            .join(participant.schedule, schedule)
            .join(author).on(author.id.eq(schedule.authorMemberId))
            .leftJoin(chapterSchool).on(chapterSchool.school.id.eq(author.schoolId))
            .leftJoin(chapter).on(chapter.id.eq(chapterSchool.chapter.id))
            .where(scheduleAuthorScopeCondition(scope, author, chapter)
                .and(periodCondition(participant.updatedAt, query))
                .and(participant.attendance.status.isNotNull()))
            .groupBy(participant.attendance.status)
            .fetch();

        Map<AttendanceStatus, Long> result = new EnumMap<>(AttendanceStatus.class);
        for (Tuple row : rows) {
            result.put(row.get(participant.attendance.status), defaultLong(row.get(statusCount)));
        }
        return result;
    }

    private AdminOperationsOverviewInfo.StudyGroupStatusInfo getStudyGroupStatus(
        AdminAnalyticsScope scope,
        AdminOperationsOverviewQuery query
    ) {
        return AdminOperationsOverviewInfo.StudyGroupStatusInfo.of(
            countStudyGroups(scope),
            countStudyGroupSchedules(scope, query)
        );
    }

    private long countStudyGroups(AdminAnalyticsScope scope) {
        QStudyGroup studyGroup = new QStudyGroup("operationsStudyGroup");
        JPAQuery<Long> countQuery = queryFactory
            .select(studyGroup.id.countDistinct())
            .from(studyGroup);
        BooleanBuilder condition = studyGroupCondition(scope, studyGroup, countQuery);

        Long count = countQuery
            .where(condition)
            .fetchOne();
        return defaultLong(count);
    }

    private long countStudyGroupSchedules(AdminAnalyticsScope scope, AdminOperationsOverviewQuery query) {
        QStudyGroupSchedule studyGroupSchedule = new QStudyGroupSchedule("operationsStudyGroupSchedule");
        QStudyGroup studyGroup = new QStudyGroup("operationsScheduledStudyGroup");
        JPAQuery<Long> countQuery = queryFactory
            .select(studyGroupSchedule.id.countDistinct())
            .from(studyGroupSchedule)
            .join(studyGroup).on(studyGroup.id.eq(studyGroupSchedule.studyGroupId));
        BooleanBuilder condition = studyGroupCondition(scope, studyGroup, countQuery)
            .and(periodCondition(studyGroupSchedule.createdAt, query));

        Long count = countQuery
            .where(condition)
            .fetchOne();
        return defaultLong(count);
    }

    private List<AdminOperationsOverviewInfo.SignupBucketInfo> listSignupBuckets(
        AdminAnalyticsScope scope,
        AdminOperationsOverviewQuery query
    ) {
        QChallenger challenger = new QChallenger("operationsSignupChallenger");
        QMember member = new QMember("operationsSignupMember");
        QChapterSchool chapterSchool = new QChapterSchool("operationsSignupChapterSchool");
        QChapter chapter = new QChapter("operationsSignupChapter");

        List<Tuple> rows = queryFactory
            .select(member.id, member.createdAt)
            .from(challenger)
            .join(member).on(member.id.eq(challenger.memberId))
            .leftJoin(chapterSchool).on(chapterSchool.school.id.eq(member.schoolId))
            .leftJoin(chapter).on(chapter.id.eq(chapterSchool.chapter.id)
                .and(chapter.gisu.id.eq(challenger.gisuId)))
            .where(challengerScopeCondition(scope, challenger, member, chapterSchool, chapter)
                .and(periodCondition(member.createdAt, query)))
            .groupBy(member.id, member.createdAt)
            .fetch();

        Map<LocalDate, Long> countsByDate = rows.stream()
            .map(row -> row.get(member.createdAt))
            .collect(Collectors.groupingBy(
                createdAt -> createdAt.atZone(DASHBOARD_ZONE).toLocalDate(),
                TreeMap::new,
                Collectors.counting()
            ));

        return countsByDate.entrySet()
            .stream()
            .map(entry -> AdminOperationsOverviewInfo.SignupBucketInfo.of(entry.getKey(), entry.getValue()))
            .toList();
    }

    private BooleanBuilder schoolScopeCondition(
        AdminAnalyticsScope scope,
        QChapter chapter,
        QSchool school
    ) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(chapter.gisu.id.eq(scope.gisuId()));
        if (scope.chapterId() != null) {
            builder.and(chapter.id.eq(scope.chapterId()));
        }
        if (scope.schoolId() != null) {
            builder.and(school.id.eq(scope.schoolId()));
        }
        return builder;
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
        // chapter_school 의 다중-기수 중복 행을 dedup. orphan 학교(매핑 없음)는 보존한다.
        builder.and(chapterMatchedOrNoMapping(chapterSchool, chapter));
        if (scope.chapterId() != null) {
            builder.and(chapter.id.eq(scope.chapterId()));
        }
        if (scope.schoolId() != null) {
            builder.and(member.schoolId.eq(scope.schoolId()));
        }
        if (scope.responsiblePart() != null) {
            builder.and(challenger.part.eq(scope.responsiblePart()));
        }
        return builder;
    }

    private BooleanBuilder scheduleScopeCondition(
        AdminAnalyticsScope scope,
        AdminOperationsOverviewQuery query,
        QSchedule schedule,
        QMember author,
        QChapter chapter
    ) {
        return scheduleScopeCondition(scope, query.from(), query.to(), schedule, author, chapter);
    }

    private BooleanBuilder scheduleScopeCondition(
        AdminAnalyticsScope scope,
        Instant from,
        Instant to,
        QSchedule schedule,
        QMember author,
        QChapter chapter
    ) {
        return scheduleAuthorScopeCondition(scope, author, chapter)
            .and(periodCondition(schedule.createdAt, from, to));
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

    private BooleanBuilder studyGroupCondition(
        AdminAnalyticsScope scope,
        QStudyGroup studyGroup,
        JPAQuery<Long> query
    ) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(studyGroup.gisuId.eq(scope.gisuId()));
        if (scope.responsiblePart() != null) {
            builder.and(studyGroup.part.eq(scope.responsiblePart()));
        }
        if (scope.chapterId() == null && scope.schoolId() == null) {
            return builder;
        }

        QStudyGroupMember studyGroupMember = new QStudyGroupMember("operationsStudyGroupMember");
        QMember member = new QMember("operationsStudyGroupMemberMember");
        QChapterSchool chapterSchool = new QChapterSchool("operationsStudyGroupChapterSchool");
        QChapter chapter = new QChapter("operationsStudyGroupChapter");

        query.leftJoin(studyGroupMember).on(studyGroupMember.studyGroup.id.eq(studyGroup.id))
            .leftJoin(member).on(member.id.eq(studyGroupMember.memberId))
            .leftJoin(chapterSchool).on(chapterSchool.school.id.eq(member.schoolId))
            .leftJoin(chapter).on(chapter.id.eq(chapterSchool.chapter.id)
                .and(chapter.gisu.id.eq(studyGroup.gisuId)));

        if (scope.chapterId() != null) {
            builder.and(chapter.id.eq(scope.chapterId()));
        }
        if (scope.schoolId() != null) {
            builder.and(member.schoolId.eq(scope.schoolId()));
        }
        return builder;
    }

    private BooleanBuilder periodCondition(DateTimePath<Instant> path, AdminOperationsOverviewQuery query) {
        return periodCondition(path, query.from(), query.to());
    }

    private BooleanBuilder periodCondition(DateTimePath<Instant> path, Instant from, Instant to) {
        return new BooleanBuilder()
            .and(path.goe(from))
            .and(path.lt(to));
    }

    private Map<ChallengerPart, Long> emptyPartCounts() {
        Map<ChallengerPart, Long> counts = new EnumMap<>(ChallengerPart.class);
        for (ChallengerPart part : ChallengerPart.values()) {
            counts.put(part, 0L);
        }
        return counts;
    }

    private long defaultLong(Long value) {
        return value != null ? value : 0L;
    }

    private double defaultDouble(Double value) {
        return value != null ? value : 0.0;
    }

    private record ChapterKey(
        Long chapterId,
        String chapterName
    ) {

        private static ChapterKey from(Tuple row, QChapter chapter) {
            return new ChapterKey(row.get(chapter.id), row.get(chapter.name));
        }
    }

    private record SchoolKey(
        Long schoolId,
        String schoolName
    ) {

        private static SchoolKey from(Tuple row, QSchool school) {
            return new SchoolKey(row.get(school.id), row.get(school.name));
        }
    }

    public AdminOperationsAttendanceChaptersInfo getAttendanceByChapters(AdminAnalyticsScope scope, Instant from, Instant to) {
        QScheduleParticipant participant = new QScheduleParticipant("chapParticipant");
        QSchedule schedule = new QSchedule("chapSchedule");
        QMember scheduleAuthor = new QMember("chapScheduleAuthor");
        QChapterSchool authorCs = new QChapterSchool("chapAuthorCs");
        QChapter authorChapter = new QChapter("chapAuthorChapter");
        QMember participantMember = new QMember("chapParticipantMember");
        QChapterSchool participantCs = new QChapterSchool("chapParticipantCs");
        QChapter participantChapter = new QChapter("chapParticipantChapter");
        QSchool participantSchool = new QSchool("chapParticipantSchool");

        NumberExpression<Long> totalExpr = participant.id.count();
        NumberExpression<Long> attendedExpr = new CaseBuilder()
            .when(participant.attendance.status.in(
                AttendanceStatus.PRESENT, AttendanceStatus.LATE, AttendanceStatus.EXCUSED
            ))
            .then(1L)
            .otherwise(0L)
            .sum();

        List<Tuple> rows = queryFactory
            .select(
                participantChapter.id,
                participantChapter.name,
                participantSchool.id,
                participantSchool.name,
                totalExpr,
                attendedExpr
            )
            .from(participant)
            .join(participant.schedule, schedule)
            .join(scheduleAuthor).on(scheduleAuthor.id.eq(schedule.authorMemberId))
            .leftJoin(authorCs).on(authorCs.school.id.eq(scheduleAuthor.schoolId))
            .leftJoin(authorChapter).on(authorChapter.id.eq(authorCs.chapter.id)
                .and(authorChapter.gisu.id.eq(scope.gisuId())))
            .join(participantMember).on(participantMember.id.eq(participant.memberId))
            .leftJoin(participantCs).on(participantCs.school.id.eq(participantMember.schoolId))
            .leftJoin(participantChapter).on(participantChapter.id.eq(participantCs.chapter.id)
                .and(participantChapter.gisu.id.eq(scope.gisuId())))
            .leftJoin(participantSchool).on(participantSchool.id.eq(participantMember.schoolId))
            .where(
                scheduleAuthorScopeCondition(scope, scheduleAuthor, authorChapter)
                    .and(chapterMatchedOrNoMapping(authorCs, authorChapter))
                    .and(chapterMatchedOrNoMapping(participantCs, participantChapter))
                    .and(periodCondition(schedule.startsAt, from, to))
                    .and(schedule.policy.attendanceGraceMinutes.isNotNull())
            )
            .groupBy(
                participantChapter.id, participantChapter.name,
                participantSchool.id, participantSchool.name
            )
            .orderBy(participantChapter.name.asc(), participantSchool.name.asc())
            .fetch();

        Map<ChapterKey, Map<SchoolKey, long[]>> chapterMap = new LinkedHashMap<>();
        for (Tuple row : rows) {
            ChapterKey chapterKey = ChapterKey.from(row, participantChapter);
            SchoolKey schoolKey = SchoolKey.from(row, participantSchool);
            long total = defaultLong(row.get(totalExpr));
            long attended = defaultLong(row.get(attendedExpr));
            chapterMap.computeIfAbsent(chapterKey, k -> new LinkedHashMap<>())
                .put(schoolKey, new long[]{total, attended});
        }

        List<AdminOperationsAttendanceChaptersInfo.ChapterAttendanceInfo> chapters = chapterMap.entrySet().stream()
            .map(chapterEntry -> {
                List<AdminOperationsAttendanceChaptersInfo.SchoolAttendanceInfo> schools = chapterEntry.getValue()
                    .entrySet().stream()
                    .map(e -> AdminOperationsAttendanceChaptersInfo.SchoolAttendanceInfo.of(
                        e.getKey().schoolId(), e.getKey().schoolName(), e.getValue()[0], e.getValue()[1]
                    ))
                    .toList();
                long chapterTotal = schools.stream().mapToLong(AdminOperationsAttendanceChaptersInfo.SchoolAttendanceInfo::totalParticipantCount).sum();
                long chapterAttended = schools.stream().mapToLong(AdminOperationsAttendanceChaptersInfo.SchoolAttendanceInfo::attendedCount).sum();
                return AdminOperationsAttendanceChaptersInfo.ChapterAttendanceInfo.of(
                    chapterEntry.getKey().chapterId(), chapterEntry.getKey().chapterName(),
                    chapterTotal, chapterAttended, schools
                );
            })
            .toList();

        return AdminOperationsAttendanceChaptersInfo.from(chapters);
    }

    public AdminOperationsAttendanceTagsInfo getAttendanceByTags(AdminAnalyticsScope scope, Instant from, Instant to) {
        QScheduleParticipant participant = new QScheduleParticipant("tagParticipant");
        QSchedule schedule = new QSchedule("tagSchedule");
        QMember scheduleAuthor = new QMember("tagScheduleAuthor");
        QChapterSchool authorCs = new QChapterSchool("tagAuthorCs");
        QChapter authorChapter = new QChapter("tagAuthorChapter");
        EnumPath<ScheduleTag> tag = Expressions.enumPath(ScheduleTag.class, "tag");

        NumberExpression<Long> totalExpr = participant.id.count();
        NumberExpression<Long> attendedExpr = new CaseBuilder()
            .when(participant.attendance.status.in(
                AttendanceStatus.PRESENT, AttendanceStatus.LATE, AttendanceStatus.EXCUSED
            ))
            .then(1L)
            .otherwise(0L)
            .sum();
        NumberExpression<Long> scheduleCountExpr = schedule.id.countDistinct();

        List<Tuple> rows = queryFactory
            .select(tag, scheduleCountExpr, totalExpr, attendedExpr)
            .from(schedule)
            .join(schedule.tags, tag)
            .join(participant).on(participant.schedule.id.eq(schedule.id))
            .join(scheduleAuthor).on(scheduleAuthor.id.eq(schedule.authorMemberId))
            .leftJoin(authorCs).on(authorCs.school.id.eq(scheduleAuthor.schoolId))
            .leftJoin(authorChapter).on(authorChapter.id.eq(authorCs.chapter.id)
                .and(authorChapter.gisu.id.eq(scope.gisuId())))
            .where(
                scheduleAuthorScopeCondition(scope, scheduleAuthor, authorChapter)
                    .and(chapterMatchedOrNoMapping(authorCs, authorChapter))
                    .and(periodCondition(schedule.startsAt, from, to))
                    .and(schedule.policy.attendanceGraceMinutes.isNotNull())
            )
            .groupBy(tag)
            .orderBy(totalExpr.desc())
            .fetch();

        List<AdminOperationsAttendanceTagsInfo.TagAttendanceInfo> byTag = rows.stream()
            .map(row -> AdminOperationsAttendanceTagsInfo.TagAttendanceInfo.of(
                row.get(tag),
                defaultLong(row.get(scheduleCountExpr)),
                defaultLong(row.get(totalExpr)),
                defaultLong(row.get(attendedExpr))
            ))
            .toList();

        return AdminOperationsAttendanceTagsInfo.from(byTag);
    }

    public AdminOperationsAttendancePartsInfo getAttendanceByParts(AdminAnalyticsScope scope, Instant from, Instant to) {
        QScheduleParticipant participant = new QScheduleParticipant("partParticipant");
        QSchedule schedule = new QSchedule("partSchedule");
        QMember scheduleAuthor = new QMember("partScheduleAuthor");
        QChapterSchool authorCs = new QChapterSchool("partAuthorCs");
        QChapter authorChapter = new QChapter("partAuthorChapter");
        QChallenger challenger = new QChallenger("partChallenger");

        NumberExpression<Long> totalExpr = participant.id.count();
        NumberExpression<Long> attendedExpr = new CaseBuilder()
            .when(participant.attendance.status.in(
                AttendanceStatus.PRESENT, AttendanceStatus.LATE, AttendanceStatus.EXCUSED
            ))
            .then(1L)
            .otherwise(0L)
            .sum();

        BooleanBuilder condition = scheduleAuthorScopeCondition(scope, scheduleAuthor, authorChapter)
            .and(chapterMatchedOrNoMapping(authorCs, authorChapter))
            .and(periodCondition(schedule.startsAt, from, to))
            .and(schedule.policy.attendanceGraceMinutes.isNotNull());

        if (scope.responsiblePart() != null) {
            condition.and(challenger.part.eq(scope.responsiblePart()));
        }

        List<Tuple> rows = queryFactory
            .select(challenger.part, totalExpr, attendedExpr)
            .from(participant)
            .join(participant.schedule, schedule)
            .join(scheduleAuthor).on(scheduleAuthor.id.eq(schedule.authorMemberId))
            .leftJoin(authorCs).on(authorCs.school.id.eq(scheduleAuthor.schoolId))
            .leftJoin(authorChapter).on(authorChapter.id.eq(authorCs.chapter.id)
                .and(authorChapter.gisu.id.eq(scope.gisuId())))
            .join(challenger).on(challenger.memberId.eq(participant.memberId)
                .and(challenger.gisuId.eq(scope.gisuId())))
            .where(condition)
            .groupBy(challenger.part)
            .orderBy(challenger.part.asc())
            .fetch();

        List<AdminOperationsAttendancePartsInfo.PartAttendanceInfo> byPart = rows.stream()
            .map(row -> AdminOperationsAttendancePartsInfo.PartAttendanceInfo.of(
                row.get(challenger.part),
                defaultLong(row.get(totalExpr)),
                defaultLong(row.get(attendedExpr))
            ))
            .toList();

        return AdminOperationsAttendancePartsInfo.from(byPart);
    }
}
