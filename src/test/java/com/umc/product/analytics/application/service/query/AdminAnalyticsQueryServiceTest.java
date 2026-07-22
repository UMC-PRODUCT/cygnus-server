package com.umc.product.analytics.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.umc.product.analytics.application.port.in.query.dto.AdminDashboardActionQueueInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminDashboardActionQueueQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminDashboardQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminDashboardSummaryInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsOverviewInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsOverviewQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsPointsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsPointsQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsSchoolsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsSchoolsQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsSignupsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsSignupsQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsStudyGroupsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsStudyGroupsQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminRiskChallengerQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminSchoolSummaryQuery;
import com.umc.product.analytics.application.port.out.LoadAdminDashboardAnalyticsPort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsAnalyticsPort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsAttendancePort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsPointsPort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsSchoolsPort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsSignupsPort;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsStudyGroupsPort;
import com.umc.product.analytics.application.port.out.LoadAdminRiskChallengerAnalyticsPort;
import com.umc.product.analytics.application.port.out.LoadAdminSchoolAnalyticsPort;
import com.umc.product.analytics.domain.AdminAnalyticsScope;
import com.umc.product.analytics.domain.AdminAnalyticsScopeType;
import com.umc.product.common.domain.enums.ChallengerRoleType;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAnalyticsQueryService")
class AdminAnalyticsQueryServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long GISU_ID = 7L;

    @Mock
    AdminAnalyticsScopeResolver scopeResolver;

    @Mock
    LoadAdminDashboardAnalyticsPort loadAdminDashboardAnalyticsPort;

    @Mock
    LoadAdminSchoolAnalyticsPort loadAdminSchoolAnalyticsPort;

    @Mock
    LoadAdminRiskChallengerAnalyticsPort loadAdminRiskChallengerAnalyticsPort;

    @Mock
    LoadAdminOperationsAnalyticsPort loadAdminOperationsAnalyticsPort;

    @Mock
    LoadAdminOperationsSchoolsPort loadAdminOperationsSchoolsPort;

    @Mock
    LoadAdminOperationsPointsPort loadAdminOperationsPointsPort;

    @Mock
    LoadAdminOperationsAttendancePort loadAdminOperationsAttendancePort;

    @Mock
    LoadAdminOperationsStudyGroupsPort loadAdminOperationsStudyGroupsPort;

    @Mock
    LoadAdminOperationsSignupsPort loadAdminOperationsSignupsPort;

    @InjectMocks
    AdminAnalyticsQueryService sut;

    @Test
    @DisplayName("summary 중앙 운영진은 전체 스코프의 KPI를 조회한다")
    void summary_중앙_운영진은_전체_스코프의_KPI를_조회한다() {
        AdminDashboardQuery query = AdminDashboardQuery.of(MEMBER_ID, GISU_ID, null, null);
        AdminAnalyticsScope scope = centralScope();
        AdminDashboardSummaryInfo expected = AdminDashboardSummaryInfo.of(
            10L,
            2L,
            100.0,
            3L,
            1L,
            AdminDashboardSummaryInfo.PointSumInfo.of(12L, -4L),
            Map.of()
        );
        given(scopeResolver.resolve(MEMBER_ID, GISU_ID, null, null, null)).willReturn(scope);
        given(loadAdminDashboardAnalyticsPort.getSummary(scope)).willReturn(expected);

        AdminDashboardSummaryInfo actual = sut.getSummary(query);

        assertThat(actual).isEqualTo(expected);
        then(loadAdminDashboardAnalyticsPort).should().getSummary(scope);
    }

    @Test
    @DisplayName("summary 학교 운영진은 본인 학교 데이터만 조회한다")
    void summary_학교_운영진은_본인_학교_데이터만_조회한다() {
        AdminDashboardQuery query = AdminDashboardQuery.of(MEMBER_ID, GISU_ID, null, null);
        AdminAnalyticsScope scope = AdminAnalyticsScope.of(
            AdminAnalyticsScopeType.SCHOOL,
            GISU_ID,
            null,
            30L,
            null,
            ChallengerRoleType.SCHOOL_PRESIDENT
        );
        AdminDashboardSummaryInfo expected = AdminDashboardSummaryInfo.empty();
        given(scopeResolver.resolve(MEMBER_ID, GISU_ID, null, null, null)).willReturn(scope);
        given(loadAdminDashboardAnalyticsPort.getSummary(scope)).willReturn(expected);

        AdminDashboardSummaryInfo actual = sut.getSummary(query);

        assertThat(actual).isEqualTo(expected);
        then(loadAdminDashboardAnalyticsPort).should().getSummary(scope);
    }

    @Test
    @DisplayName("actionQueue 출석 승인 대기와 위험군, 수료 임박 항목을 조회한다")
    void actionQueue_출석_승인_대기와_위험군_수료_임박_항목을_조회한다() {
        AdminDashboardActionQueueQuery query = AdminDashboardActionQueueQuery.of(MEMBER_ID, GISU_ID, -8);
        AdminAnalyticsScope scope = centralScope();
        AdminDashboardActionQueueInfo expected = AdminDashboardActionQueueInfo.of(4L, 3L, 2L);
        given(scopeResolver.resolve(MEMBER_ID, GISU_ID, null, null, null)).willReturn(scope);
        given(loadAdminDashboardAnalyticsPort.getActionQueue(scope, -8)).willReturn(expected);

        AdminDashboardActionQueueInfo actual = sut.getActionQueue(query);

        assertThat(actual.pendingAttendanceDecisionCount()).isEqualTo(4L);
        assertThat(actual.newRiskMemberCountThisWeek()).isEqualTo(3L);
        assertThat(actual.upcomingGraduationCount()).isEqualTo(2L);
        then(loadAdminDashboardAnalyticsPort).should().getActionQueue(scope, -8);
    }

    @Test
    @DisplayName("operationsOverview는 권한 스코프를 적용해 운영 현황을 조회한다")
    void operationsOverview는_권한_스코프를_적용해_운영_현황을_조회한다() {
        Instant from = Instant.parse("2026-05-01T00:00:00Z");
        Instant to = Instant.parse("2026-05-13T00:00:00Z");
        AdminOperationsOverviewQuery query = AdminOperationsOverviewQuery.of(MEMBER_ID, GISU_ID, from, to);
        AdminAnalyticsScope scope = centralScope();
        AdminOperationsOverviewInfo expected = AdminOperationsOverviewInfo.of(
            java.util.List.of(),
            java.util.List.of(),
            AdminOperationsOverviewInfo.ScheduleAttendanceStatusInfo.of(3L, 2L, 8L, Map.of()),
            AdminOperationsOverviewInfo.StudyGroupStatusInfo.of(4L, 6L),
            java.util.List.of(AdminOperationsOverviewInfo.SignupBucketInfo.of(LocalDate.parse("2026-05-01"), 5L))
        );
        given(scopeResolver.resolve(MEMBER_ID, GISU_ID, null, null, null)).willReturn(scope);
        given(loadAdminOperationsAnalyticsPort.getOperationsOverview(scope, query)).willReturn(expected);

        AdminOperationsOverviewInfo actual = sut.getOperationsOverview(query);

        assertThat(actual).isEqualTo(expected);
        then(loadAdminOperationsAnalyticsPort).should().getOperationsOverview(scope, query);
    }

    @Test
    @DisplayName("context·학교·위험군과 분리된 운영 지표를 동일한 권한 스코프로 조회한다")
    void delegates_remaining_analytics_queries_with_resolved_scope() {
        Instant from = Instant.parse("2026-05-01T00:00:00Z");
        Instant to = Instant.parse("2026-05-13T00:00:00Z");
        AdminAnalyticsScope scope = centralScope();
        given(scopeResolver.resolve(MEMBER_ID, null, null, null, null)).willReturn(scope);
        assertThat(sut.getContext(MEMBER_ID).gisuId()).isEqualTo(GISU_ID);

        PageRequest pageable = PageRequest.of(0, 10);
        AdminSchoolSummaryQuery schoolQuery = AdminSchoolSummaryQuery.of(
            MEMBER_ID, GISU_ID, 2L, null, null, pageable, null
        );
        AdminRiskChallengerQuery riskQuery = AdminRiskChallengerQuery.of(
            MEMBER_ID, GISU_ID, 2L, 3L, null, pageable
        );
        given(scopeResolver.resolve(MEMBER_ID, GISU_ID, 2L, null, null)).willReturn(scope);
        given(scopeResolver.resolve(MEMBER_ID, GISU_ID, 2L, 3L, null)).willReturn(scope);
        given(loadAdminSchoolAnalyticsPort.getSchoolSummaries(scope, schoolQuery))
            .willReturn(new PageImpl<>(java.util.List.of(), pageable, 0));
        given(loadAdminRiskChallengerAnalyticsPort.getRiskChallengers(scope, riskQuery))
            .willReturn(new PageImpl<>(java.util.List.of(), pageable, 0));
        assertThat(sut.getSchoolSummaries(schoolQuery)).isEmpty();
        assertThat(sut.getRiskChallengers(riskQuery)).isEmpty();

        AdminOperationsSchoolsQuery schoolsQuery = AdminOperationsSchoolsQuery.of(MEMBER_ID, GISU_ID);
        AdminOperationsPointsQuery pointsQuery = AdminOperationsPointsQuery.of(MEMBER_ID, GISU_ID, from, to);
        AdminOperationsAttendanceQuery attendanceQuery = AdminOperationsAttendanceQuery.of(MEMBER_ID, GISU_ID, from, to);
        AdminOperationsStudyGroupsQuery studyGroupsQuery = AdminOperationsStudyGroupsQuery.of(MEMBER_ID, GISU_ID, from, to);
        AdminOperationsSignupsQuery signupsQuery = AdminOperationsSignupsQuery.of(MEMBER_ID, GISU_ID, from, to);
        given(scopeResolver.resolve(MEMBER_ID, GISU_ID, null, null, null)).willReturn(scope);
        AdminOperationsSchoolsInfo schools = AdminOperationsSchoolsInfo.from(java.util.List.of());
        AdminOperationsPointsInfo points = AdminOperationsPointsInfo.from(java.util.List.of());
        AdminOperationsAttendanceInfo attendance = AdminOperationsAttendanceInfo.of(0, 0, 0, Map.of());
        AdminOperationsStudyGroupsInfo groups = AdminOperationsStudyGroupsInfo.of(0, 0);
        AdminOperationsSignupsInfo signups = AdminOperationsSignupsInfo.from(java.util.List.of());
        given(loadAdminOperationsSchoolsPort.getOperationsSchools(scope)).willReturn(schools);
        given(loadAdminOperationsPointsPort.getOperationsPoints(scope, from, to)).willReturn(points);
        given(loadAdminOperationsAttendancePort.getOperationsAttendance(scope, from, to)).willReturn(attendance);
        given(loadAdminOperationsStudyGroupsPort.getOperationsStudyGroups(scope, from, to)).willReturn(groups);
        given(loadAdminOperationsSignupsPort.getOperationsSignups(scope, from, to)).willReturn(signups);

        assertThat(sut.getOperationsSchools(schoolsQuery)).isSameAs(schools);
        assertThat(sut.getOperationsPoints(pointsQuery)).isSameAs(points);
        assertThat(sut.getOperationsAttendance(attendanceQuery)).isSameAs(attendance);
        assertThat(sut.getOperationsStudyGroups(studyGroupsQuery)).isSameAs(groups);
        assertThat(sut.getOperationsSignups(signupsQuery)).isSameAs(signups);
    }

    private AdminAnalyticsScope centralScope() {
        return AdminAnalyticsScope.of(
            AdminAnalyticsScopeType.CENTRAL,
            GISU_ID,
            null,
            null,
            null,
            ChallengerRoleType.CENTRAL_PRESIDENT
        );
    }
}
