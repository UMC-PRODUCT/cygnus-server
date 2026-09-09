package com.umc.product.analytics.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.umc.product.analytics.application.port.in.query.GetAdminDashboardActionQueueUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminDashboardContextUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminDashboardSummaryUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminOperationsAttendanceUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminOperationsOverviewUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminOperationsPointsUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminOperationsSchoolsUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminOperationsSignupsUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminOperationsStudyGroupsUseCase;
import com.umc.product.analytics.application.port.in.query.GetAdminRiskChallengerUseCase;
import com.umc.product.analytics.application.port.in.query.dto.AdminDashboardActionQueueInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminDashboardContextInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminDashboardSummaryInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsOverviewInfo;
import com.umc.product.analytics.domain.AdminAnalyticsRoleType;
import com.umc.product.analytics.domain.AdminAnalyticsScopeType;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.global.config.JacksonConfig;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.schedule.domain.enums.AttendanceStatus;

@WebMvcTest(controllers = AdminDashboardController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminDashboardController")
class AdminDashboardControllerTest {

    private static final Long MEMBER_ID = 1L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    GetAdminDashboardSummaryUseCase getAdminDashboardSummaryUseCase;

    @MockitoBean
    GetAdminDashboardActionQueueUseCase getAdminDashboardActionQueueUseCase;

    @MockitoBean
    GetAdminDashboardContextUseCase getAdminDashboardContextUseCase;

    @MockitoBean
    GetAdminRiskChallengerUseCase getAdminRiskChallengerUseCase;

    @MockitoBean
    GetAdminOperationsOverviewUseCase getAdminOperationsOverviewUseCase;

    @MockitoBean
    GetAdminOperationsSchoolsUseCase getAdminOperationsSchoolsUseCase;

    @MockitoBean
    GetAdminOperationsPointsUseCase getAdminOperationsPointsUseCase;

    @MockitoBean
    GetAdminOperationsAttendanceUseCase getAdminOperationsAttendanceUseCase;

    @MockitoBean
    GetAdminOperationsStudyGroupsUseCase getAdminOperationsStudyGroupsUseCase;

    @MockitoBean
    GetAdminOperationsSignupsUseCase getAdminOperationsSignupsUseCase;

    @BeforeEach
    void setUpSecurityContext() {
        MemberPrincipal principal = MemberPrincipal.builder().memberId(MEMBER_ID).build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("대시보드 summary API 응답")
    void 대시보드_summary_API_응답() throws Exception {
        given(getAdminDashboardSummaryUseCase.getSummary(any())).willReturn(AdminDashboardSummaryInfo.of(
            10L,
            2L,
            100.0,
            3L,
            1L,
            AdminDashboardSummaryInfo.PointSumInfo.of(12L, -4L),
            Map.of()
        ));

        mockMvc.perform(get("/api/v1/analytics/admin/dashboard/summary")
                .param("gisuId", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.activeChallengerCount").value(10L))
            .andExpect(jsonPath("$.result.pendingApplicationCount").doesNotExist())
            .andExpect(jsonPath("$.result.projectInProgressCount").doesNotExist());
    }

    @Test
    @DisplayName("대시보드 actionQueue API는 대상 도메인의 처리 대기 항목만 반환한다")
    void 대시보드_actionQueue_API_응답() throws Exception {
        given(getAdminDashboardActionQueueUseCase.getActionQueue(any()))
            .willReturn(AdminDashboardActionQueueInfo.of(4L, 3L, 5L));

        mockMvc.perform(get("/api/v1/analytics/admin/dashboard/action-queue")
                .param("gisuId", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.pendingAttendanceDecisionCount").value(4L))
            .andExpect(jsonPath("$.result.newRiskMemberCountThisWeek").value(3L))
            .andExpect(jsonPath("$.result.upcomingGraduationCount").value(5L))
            .andExpect(jsonPath("$.result.pendingApplicationCount").doesNotExist())
            .andExpect(jsonPath("$.result.unsentNoticeCount").doesNotExist());
    }

    @Test
    @DisplayName("대시보드 context API 응답")
    void 대시보드_context_API_응답() throws Exception {
        given(getAdminDashboardContextUseCase.getContext(MEMBER_ID)).willReturn(new AdminDashboardContextInfo(
            AdminAnalyticsRoleType.SUPER_ADMIN,
            7L,
            null,
            null,
            null,
            AdminAnalyticsScopeType.CENTRAL
        ));

        mockMvc.perform(get("/api/v1/analytics/admin/dashboard/context"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.roleType").value("SUPER_ADMIN"))
            .andExpect(jsonPath("$.result.scopeType").value("CENTRAL"));
    }

    @Test
    @DisplayName("대시보드 riskChallengers API 응답")
    void 대시보드_riskChallengers_API_응답() throws Exception {
        given(getAdminRiskChallengerUseCase.getRiskChallengers(any())).willReturn(Page.empty());

        mockMvc.perform(get("/api/v1/analytics/admin/dashboard/risk-challengers")
                .param("gisuId", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content").isArray());
    }

    @Test
    @DisplayName("대시보드 operations API 응답")
    void 대시보드_operations_API_응답() throws Exception {
        given(getAdminOperationsOverviewUseCase.getOperationsOverview(any())).willReturn(
            AdminOperationsOverviewInfo.of(
                java.util.List.of(AdminOperationsOverviewInfo.ChapterSchoolStatusInfo.of(
                    10L,
                    "중앙",
                    java.util.List.of(AdminOperationsOverviewInfo.SchoolChallengerStatusInfo.of(
                        20L,
                        "가천대학교",
                        12L,
                        Map.of(ChallengerPart.SPRINGBOOT, 7L)
                    ))
                )),
                java.util.List.of(AdminOperationsOverviewInfo.ChapterPartPointGrantStatusInfo.of(
                    10L,
                    "중앙",
                    ChallengerPart.SPRINGBOOT,
                    3L,
                    -12.0
                )),
                AdminOperationsOverviewInfo.ScheduleAttendanceStatusInfo.of(
                    5L,
                    4L,
                    8L,
                    Map.of(AttendanceStatus.PRESENT, 6L)
                ),
                AdminOperationsOverviewInfo.StudyGroupStatusInfo.of(9L, 11L),
                java.util.List.of(AdminOperationsOverviewInfo.SignupBucketInfo.of(LocalDate.parse("2026-05-01"), 2L))
            )
        );

        mockMvc.perform(get("/api/v1/analytics/admin/dashboard/operations")
                .param("gisuId", "7")
                .param("from", "2026-05-01T00:00:00Z")
                .param("to", "2026-05-13T00:00:00Z"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.chapterSchoolStatuses[0].chapterId").value(10L))
            .andExpect(jsonPath("$.result.chapterSchoolStatuses[0].schools[0].challengerPartCounts.SPRINGBOOT").value(7L))
            .andExpect(jsonPath("$.result.pointGrantStatuses[0].grantCount").value(3L))
            .andExpect(jsonPath("$.result.scheduleAttendanceStatus.attendanceRecordCount").value(8L))
            .andExpect(jsonPath("$.result.studyGroupStatus.studyGroupCount").value(9L))
            .andExpect(jsonPath("$.result.signupBuckets[0].count").value(2L));
    }
}
