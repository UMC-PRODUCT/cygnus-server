package com.umc.product.analytics.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsOverviewQuery;
import com.umc.product.analytics.domain.AdminAnalyticsScope;
import com.umc.product.analytics.domain.AdminAnalyticsScopeType;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.challenger.domain.ChallengerPoint;
import com.umc.product.challenger.domain.enums.PointType;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.ChapterSchool;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;
import com.umc.product.organization.domain.StudyGroup;
import com.umc.product.organization.domain.StudyGroupSchedule;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.ScheduleParticipant;
import com.umc.product.schedule.domain.ScheduleParticipantAttendance;
import com.umc.product.schedule.domain.enums.AttendanceStatus;
import com.umc.product.schedule.domain.enums.ScheduleTag;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import(AdminOperationsAnalyticsQueryRepository.class)
@DisplayName("AdminOperationsAnalyticsQueryRepository")
class AdminOperationsAnalyticsQueryRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    AdminOperationsAnalyticsQueryRepository sut;

    Long gisuId;
    Long chapterId;
    Long schoolId;
    Instant from;
    Instant to;

    @BeforeEach
    void setUp() {
        Gisu gisu = em.persist(Gisu.create(
            88L, Instant.now().minusSeconds(3600), Instant.now().plusSeconds(86400), false
        ));
        Chapter chapter = em.persist(Chapter.create(gisu, "운영 지부"));
        School school = em.persist(School.create("운영 학교", null));
        em.persist(ChapterSchool.create(chapter, school));
        Member member = em.persist(Member.create("회원", "닉네임", "operations@example.com", school.getId(), null));
        Challenger challenger = em.persist(new Challenger(member.getId(), ChallengerPart.SPRINGBOOT, gisu.getId()));
        em.persist(ChallengerPoint.create(challenger, PointType.BLOG_CHALLENGE, 3, "가점"));

        Schedule schedule = em.persist(schedule(member.getId()));
        em.persist(ScheduleParticipant.builder()
            .schedule(schedule)
            .memberId(member.getId())
            .attendance(ScheduleParticipantAttendance.create(null, true, null, AttendanceStatus.PRESENT))
            .build());

        StudyGroup group = em.persist(StudyGroup.create(
            "운영 스터디", gisu.getId(), ChallengerPart.SPRINGBOOT,
            Set.of(member.getId()), Set.of(member.getId())
        ));
        em.persist(StudyGroupSchedule.builder()
            .studyGroupId(group.getId())
            .scheduleId(schedule.getId())
            .weeklyCurriculumId(1L)
            .build());
        em.flush();
        em.clear();

        gisuId = gisu.getId();
        chapterId = chapter.getId();
        schoolId = school.getId();
        from = Instant.now().minusSeconds(86400);
        to = Instant.now().plusSeconds(86400);
    }

    @Test
    @DisplayName("중앙 운영 지표는 학교·포인트·출석·스터디·가입을 동일 기간으로 집계한다")
    void central_scope_aggregates_all_operations_metrics() {
        AdminAnalyticsScope scope = scope(AdminAnalyticsScopeType.CENTRAL, null, null, null,
            ChallengerRoleType.CENTRAL_PRESIDENT);
        AdminOperationsOverviewQuery query = AdminOperationsOverviewQuery.of(1L, gisuId, from, to);

        var overview = sut.getOperationsOverview(scope, query);
        var schools = sut.getOperationsSchools(scope);
        var points = sut.getOperationsPoints(scope, from, to);
        var attendance = sut.getOperationsAttendance(scope, from, to);
        var groups = sut.getOperationsStudyGroups(scope, from, to);
        var signups = sut.getOperationsSignups(scope, from, to);

        assertThat(overview.chapterSchoolStatuses()).hasSize(1);
        assertThat(overview.pointGrantStatuses()).hasSize(1);
        assertThat(schools.chapters()).hasSize(1);
        assertThat(points.pointGrantStatuses()).singleElement().satisfies(row -> {
            assertThat(row.grantCount()).isOne();
            assertThat(row.pointSum()).isEqualTo(3.0);
        });
        assertThat(attendance.scheduleCount()).isOne();
        assertThat(attendance.attendanceRequiredScheduleCount()).isOne();
        assertThat(attendance.attendanceRecordCount()).isOne();
        assertThat(groups.studyGroupCount()).isOne();
        assertThat(groups.studyGroupScheduleCount()).isOne();
        assertThat(signups.signupBuckets()).singleElement().satisfies(bucket -> assertThat(bucket.count()).isOne());
    }

    @Test
    @DisplayName("지부·학교·파트 스코프와 기간 밖 데이터는 각 집계에서 fail-closed 한다")
    void scoped_and_out_of_period_queries_are_fail_closed() {
        AdminAnalyticsScope partScope = scope(
            AdminAnalyticsScopeType.SCHOOL_PART, chapterId, schoolId, ChallengerPart.SPRINGBOOT,
            ChallengerRoleType.SCHOOL_PART_LEADER
        );
        assertThat(sut.getOperationsSchools(partScope).chapters()).hasSize(1);
        assertThat(sut.getOperationsPoints(partScope, from, to).pointGrantStatuses()).hasSize(1);
        assertThat(sut.getOperationsStudyGroups(partScope, from, to).studyGroupCount()).isOne();
        assertThat(sut.getOperationsSignups(partScope, from, to).signupBuckets()).hasSize(1);

        AdminAnalyticsScope missingSchool = scope(
            AdminAnalyticsScopeType.SCHOOL, chapterId, Long.MAX_VALUE, null,
            ChallengerRoleType.SCHOOL_PRESIDENT
        );
        assertThat(sut.getOperationsSchools(missingSchool).chapters()).isEmpty();
        assertThat(sut.getOperationsAttendance(missingSchool, from, to).scheduleCount()).isZero();
        assertThat(sut.getOperationsStudyGroups(missingSchool, from, to).studyGroupCount()).isZero();

        Instant futureFrom = Instant.now().plusSeconds(172800);
        Instant futureTo = futureFrom.plusSeconds(3600);
        assertThat(sut.getOperationsPoints(partScope, futureFrom, futureTo).pointGrantStatuses()).isEmpty();
        assertThat(sut.getOperationsSignups(partScope, futureFrom, futureTo).signupBuckets()).isEmpty();
    }

    private Schedule schedule(Long authorMemberId) {
        Instant startsAt = Instant.now().plusSeconds(3600);
        Instant endsAt = startsAt.plusSeconds(7200);
        return Schedule.builder()
            .name("운영 일정")
            .description("운영 지표")
            .tags(Set.of(ScheduleTag.MEETING))
            .authorMemberId(authorMemberId)
            .startsAt(startsAt)
            .endsAt(endsAt)
            .policy(Schedule.createAttendancePolicy(
                startsAt.minusSeconds(600), startsAt.plusSeconds(600),
                startsAt.plusSeconds(1200), startsAt, endsAt
            ))
            .build();
    }

    private AdminAnalyticsScope scope(
        AdminAnalyticsScopeType type,
        Long chapterId,
        Long schoolId,
        ChallengerPart part,
        ChallengerRoleType roleType
    ) {
        return AdminAnalyticsScope.of(type, gisuId, chapterId, schoolId, part, roleType);
    }
}
