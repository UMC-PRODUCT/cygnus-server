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
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.analytics.application.port.in.query.dto.AdminDashboardActionQueueInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminDashboardSummaryInfo;
import com.umc.product.analytics.domain.AdminAnalyticsScope;
import com.umc.product.analytics.domain.AdminAnalyticsScopeType;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.challenger.domain.ChallengerPoint;
import com.umc.product.challenger.domain.enums.PointType;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.ChapterSchool;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.ScheduleParticipant;
import com.umc.product.schedule.domain.ScheduleParticipantAttendance;
import com.umc.product.schedule.domain.enums.AttendanceStatus;
import com.umc.product.schedule.domain.enums.ScheduleTag;
import com.umc.product.support.PersistenceAdapterTest;

@PersistenceAdapterTest
@Import({AdminDashboardAnalyticsQueryRepository.class})
@DisplayName("AdminDashboardAnalyticsQueryRepository")
class AdminDashboardAnalyticsQueryRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    AdminDashboardAnalyticsQueryRepository sut;

    private Long gisuId;
    private Long chapterId;
    private Long schoolAId;
    private Long schoolBId;

    @BeforeEach
    void setUp() {
        // 시드 마이그레이션(V2026.02.28.06.00)이 이미 is_active=true 인 10기를 넣어둔다.
        // 부분 unique index uq_gisu_active 와 충돌하지 않도록 테스트 fixture 는 비활성 기수로 만든다.
        // 분석 쿼리는 scope.gisuId() 로 기수를 스코프하므로 is_active 값과는 무관하다.
        Gisu gisu = em.persist(Gisu.create(7L, Instant.now().minusSeconds(3600), Instant.now().plusSeconds(86400 * 30), false));
        Chapter chapter = em.persist(Chapter.create(gisu, "중앙"));
        School schoolA = em.persist(School.create("A대학교", null));
        School schoolB = em.persist(School.create("B대학교", null));
        em.persist(ChapterSchool.create(chapter, schoolA));
        em.persist(ChapterSchool.create(chapter, schoolB));
        em.flush();

        gisuId = gisu.getId();
        chapterId = chapter.getId();
        schoolAId = schoolA.getId();
        schoolBId = schoolB.getId();
    }

    @Test
    @DisplayName("summary 중앙 운영진은 전체 스코프의 KPI를 조회한다")
    void summary_중앙_운영진은_전체_스코프의_KPI를_조회한다() {
        Challenger schoolAChallenger = persistChallenger("김서버", schoolAId, ChallengerPart.SPRINGBOOT, ChallengerStatus.ACTIVE);
        persistChallenger("이웹", schoolBId, ChallengerPart.WEB, ChallengerStatus.ACTIVE);
        persistChallenger("박수료", schoolBId, ChallengerPart.DESIGN, ChallengerStatus.GRADUATED);
        persistPoint(schoolAChallenger, PointType.BLOG_CHALLENGE, null);
        persistPoint(schoolAChallenger, PointType.NO_WORKBOOK_MISSION, null);
        em.flush();
        em.clear();

        AdminDashboardSummaryInfo result = sut.getSummary(centralScope());

        assertThat(result.activeChallengerCount()).isEqualTo(2L);
        assertThat(result.activeSchoolCount()).isEqualTo(2L);
        assertThat(result.activeChapterCount()).isEqualTo(1L);
        assertThat(result.monthlyPointSum().positive()).isEqualTo(3L);
        assertThat(result.monthlyPointSum().negative()).isEqualTo(-4L);
        assertThat(result.challengerStatusDistribution().get(ChallengerStatus.ACTIVE)).isEqualTo(2L);
        assertThat(result.challengerStatusDistribution().get(ChallengerStatus.GRADUATED)).isEqualTo(1L);
        assertThat(result.challengerStatusDistribution().get(ChallengerStatus.EXPELLED)).isZero();
    }

    @Test
    @DisplayName("summary 학교 운영진은 본인 학교 데이터만 조회한다")
    void summary_학교_운영진은_본인_학교_데이터만_조회한다() {
        persistChallenger("김서버", schoolAId, ChallengerPart.SPRINGBOOT, ChallengerStatus.ACTIVE);
        persistChallenger("이웹", schoolBId, ChallengerPart.WEB, ChallengerStatus.ACTIVE);
        em.flush();
        em.clear();

        AdminDashboardSummaryInfo result = sut.getSummary(schoolScope(schoolAId));

        assertThat(result.activeChallengerCount()).isEqualTo(1L);
        assertThat(result.activeSchoolCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("actionQueue 출석 승인 대기 건수를 처리 대기로 계산한다")
    void actionQueue_출석_승인_대기_건수를_처리_대기로_계산한다() {
        Member author = persistMember("일정작성자", schoolAId);
        Schedule schedule = persistSchedule(author.getId());
        persistParticipant(schedule, 101L, AttendanceStatus.PRESENT_PENDING);
        persistParticipant(schedule, 102L, AttendanceStatus.LATE_PENDING);
        persistParticipant(schedule, 103L, AttendanceStatus.PRESENT);
        em.flush();
        em.clear();

        AdminDashboardActionQueueInfo result = sut.getActionQueue(centralScope(), -8);

        assertThat(result.pendingAttendanceDecisionCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("수료 임박과 지부·학교·파트 스코프 및 이전 주 대비 증감률을 계산한다")
    void 수료_임박_scope와_증감률을_계산한다() {
        Gisu nearEnd = em.persist(Gisu.create(
            8L, Instant.now().minusSeconds(86400), Instant.now().plusSeconds(86400 * 10), false
        ));
        Chapter nearChapter = em.persist(Chapter.create(nearEnd, "임박 지부"));
        School nearSchool = em.persist(School.create("임박 학교", null));
        em.persist(ChapterSchool.create(nearChapter, nearSchool));
        Member nearMember = em.persist(Member.create("임박", "임박닉", "near@example.com", nearSchool.getId(), null));
        em.persist(new Challenger(nearMember.getId(), ChallengerPart.WEB, nearEnd.getId()));
        em.flush();
        em.clear();

        AdminAnalyticsScope nearScope = AdminAnalyticsScope.of(
            AdminAnalyticsScopeType.CENTRAL, nearEnd.getId(), null, null, null,
            ChallengerRoleType.CENTRAL_PRESIDENT
        );
        assertThat(sut.getActionQueue(nearScope, -8).upcomingGraduationCount()).isOne();

        AdminAnalyticsScope scoped = AdminAnalyticsScope.of(
            AdminAnalyticsScopeType.SCHOOL_PART, gisuId, chapterId, schoolAId, ChallengerPart.SPRINGBOOT,
            ChallengerRoleType.SCHOOL_PART_LEADER
        );
        assertThat(sut.getSummary(scoped).activeChallengerCount()).isZero();
        assertThat(sut.getActionQueue(scoped, -8).pendingAttendanceDecisionCount()).isZero();

        Double increase = ReflectionTestUtils.invokeMethod(sut, "deltaPercent", 3L, 2L);
        assertThat(increase).isEqualTo(50.0);
    }

    private Challenger persistChallenger(
        String name,
        Long schoolId,
        ChallengerPart part,
        ChallengerStatus status
    ) {
        Member member = persistMember(name, schoolId);
        Challenger challenger = em.persist(new Challenger(member.getId(), part, gisuId));
        if (status != ChallengerStatus.ACTIVE) {
            challenger.changeStatus(status, 999L, "테스트 상태 변경");
        }
        return challenger;
    }

    private Member persistMember(String name, Long schoolId) {
        return em.persist(Member.create(name, name + "닉", name + "@example.com", schoolId, null));
    }

    private void persistPoint(Challenger challenger, PointType pointType, Integer pointValue) {
        em.persist(ChallengerPoint.create(challenger, pointType, pointValue, "테스트 포인트"));
    }

    private Schedule persistSchedule(Long authorMemberId) {
        Instant startsAt = Instant.now().plusSeconds(3600);
        Instant endsAt = startsAt.plusSeconds(7200);
        return em.persist(Schedule.builder()
            .name("운영 일정")
            .description("출석 승인 대기 집계 테스트")
            .tags(Set.of(ScheduleTag.MEETING))
            .authorMemberId(authorMemberId)
            .startsAt(startsAt)
            .endsAt(endsAt)
            .policy(Schedule.createAttendancePolicy(
                startsAt.minusSeconds(600),
                startsAt.plusSeconds(600),
                startsAt.plusSeconds(1200),
                startsAt,
                endsAt
            ))
            .build());
    }

    private void persistParticipant(Schedule schedule, Long memberId, AttendanceStatus status) {
        em.persist(ScheduleParticipant.builder()
            .schedule(schedule)
            .memberId(memberId)
            .attendance(ScheduleParticipantAttendance.create(null, true, null, status))
            .build());
    }

    private AdminAnalyticsScope centralScope() {
        return AdminAnalyticsScope.of(
            AdminAnalyticsScopeType.CENTRAL,
            gisuId,
            null,
            null,
            null,
            ChallengerRoleType.CENTRAL_PRESIDENT
        );
    }

    private AdminAnalyticsScope schoolScope(Long schoolId) {
        return AdminAnalyticsScope.of(
            AdminAnalyticsScopeType.SCHOOL,
            gisuId,
            null,
            schoolId,
            null,
            ChallengerRoleType.SCHOOL_PRESIDENT
        );
    }
}
