package com.umc.product.analytics.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.umc.product.analytics.application.port.in.query.dto.AdminDashboardActionQueueInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminDashboardSummaryInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsOverviewInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsOverviewQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsPointsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsSchoolsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsSignupsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsStudyGroupsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminRiskChallengerQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminSchoolSummaryQuery;
import com.umc.product.analytics.domain.AdminAnalyticsScope;
import com.umc.product.analytics.domain.AdminAnalyticsScopeType;
import com.umc.product.common.domain.enums.ChallengerRoleType;

@DisplayName("Admin analytics persistence adapter 위임")
class AdminAnalyticsPersistenceAdapterUnitTest {

    @Test
    @DisplayName("dashboard·operations·school·risk adapter는 query repository 결과를 그대로 반환한다")
    void delegates_all_read_ports() {
        AdminAnalyticsScope scope = AdminAnalyticsScope.of(
            AdminAnalyticsScopeType.CENTRAL, 1L, null, null, null, ChallengerRoleType.CENTRAL_PRESIDENT
        );
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-02-01T00:00:00Z");

        AdminDashboardAnalyticsQueryRepository dashboardRepository = mock(AdminDashboardAnalyticsQueryRepository.class);
        AdminDashboardSummaryInfo summary = AdminDashboardSummaryInfo.empty();
        AdminDashboardActionQueueInfo queue = AdminDashboardActionQueueInfo.of(1, 2, 3);
        given(dashboardRepository.getSummary(scope)).willReturn(summary);
        given(dashboardRepository.getActionQueue(scope, -8)).willReturn(queue);
        AdminDashboardAnalyticsPersistenceAdapter dashboard =
            new AdminDashboardAnalyticsPersistenceAdapter(dashboardRepository);
        assertThat(dashboard.getSummary(scope)).isSameAs(summary);
        assertThat(dashboard.getActionQueue(scope, -8)).isSameAs(queue);

        AdminOperationsAnalyticsQueryRepository operationsRepository = mock(AdminOperationsAnalyticsQueryRepository.class);
        AdminOperationsOverviewQuery overviewQuery = AdminOperationsOverviewQuery.of(1L, 1L, from, to);
        AdminOperationsOverviewInfo overview = mock(AdminOperationsOverviewInfo.class);
        AdminOperationsSchoolsInfo schools = AdminOperationsSchoolsInfo.from(java.util.List.of());
        AdminOperationsPointsInfo points = AdminOperationsPointsInfo.from(java.util.List.of());
        AdminOperationsAttendanceInfo attendance = AdminOperationsAttendanceInfo.of(0, 0, 0, java.util.Map.of());
        AdminOperationsStudyGroupsInfo groups = AdminOperationsStudyGroupsInfo.of(0, 0);
        AdminOperationsSignupsInfo signups = AdminOperationsSignupsInfo.from(java.util.List.of());
        given(operationsRepository.getOperationsOverview(scope, overviewQuery)).willReturn(overview);
        given(operationsRepository.getOperationsSchools(scope)).willReturn(schools);
        given(operationsRepository.getOperationsPoints(scope, from, to)).willReturn(points);
        given(operationsRepository.getOperationsAttendance(scope, from, to)).willReturn(attendance);
        given(operationsRepository.getOperationsStudyGroups(scope, from, to)).willReturn(groups);
        given(operationsRepository.getOperationsSignups(scope, from, to)).willReturn(signups);
        AdminOperationsAnalyticsPersistenceAdapter operations =
            new AdminOperationsAnalyticsPersistenceAdapter(operationsRepository);
        assertThat(operations.getOperationsOverview(scope, overviewQuery)).isSameAs(overview);
        assertThat(operations.getOperationsSchools(scope)).isSameAs(schools);
        assertThat(operations.getOperationsPoints(scope, from, to)).isSameAs(points);
        assertThat(operations.getOperationsAttendance(scope, from, to)).isSameAs(attendance);
        assertThat(operations.getOperationsStudyGroups(scope, from, to)).isSameAs(groups);
        assertThat(operations.getOperationsSignups(scope, from, to)).isSameAs(signups);

        PageRequest pageable = PageRequest.of(0, 10);
        AdminSchoolSummaryQuery schoolQuery = AdminSchoolSummaryQuery.of(1L, 1L, null, null, null, pageable, null);
        AdminSchoolAnalyticsQueryRepository schoolRepository = mock(AdminSchoolAnalyticsQueryRepository.class);
        given(schoolRepository.getSchoolSummaries(scope, schoolQuery)).willReturn(Page.empty(pageable));
        assertThat(new AdminSchoolAnalyticsPersistenceAdapter(schoolRepository)
            .getSchoolSummaries(scope, schoolQuery)).isEmpty();

        AdminRiskChallengerQuery riskQuery = AdminRiskChallengerQuery.of(1L, 1L, null, null, null, pageable);
        AdminRiskChallengerAnalyticsQueryRepository riskRepository =
            mock(AdminRiskChallengerAnalyticsQueryRepository.class);
        given(riskRepository.getRiskChallengers(scope, riskQuery)).willReturn(Page.empty(pageable));
        assertThat(new AdminRiskChallengerAnalyticsPersistenceAdapter(riskRepository)
            .getRiskChallengers(scope, riskQuery)).isEmpty();
    }
}
