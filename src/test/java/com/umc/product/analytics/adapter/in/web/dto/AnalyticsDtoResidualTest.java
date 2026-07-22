package com.umc.product.analytics.adapter.in.web.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.analytics.adapter.in.web.dto.response.AdminRiskChallengerResponse;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsAttendanceQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsOverviewQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsPointsQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsSignupsQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsStudyGroupsQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminRiskChallengerInfo;
import com.umc.product.analytics.domain.AdminAnalyticsRoleType;
import com.umc.product.analytics.domain.AdminAnalyticsScope;
import com.umc.product.analytics.domain.AdminAnalyticsScopeType;
import com.umc.product.analytics.domain.AdminAnalyticsSort;
import com.umc.product.analytics.domain.AnalyticsDomainException;
import com.umc.product.analytics.domain.AnalyticsErrorCode;
import com.umc.product.challenger.domain.enums.PointType;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;

@DisplayName("Analytics DTO·enum 잔여 경계")
class AnalyticsDtoResidualTest {

    @Test
    @DisplayName("운영 기간 query는 기본 30일을 만들고 역전·동일 기간을 거부한다")
    void period_queries_default_and_reject_invalid_ranges() {
        assertThat(AdminOperationsOverviewQuery.of(1L, 2L, null, null).from()).isBefore(Instant.now());
        assertThat(AdminOperationsPointsQuery.of(1L, 2L, null, null).from()).isBefore(Instant.now());
        assertThat(AdminOperationsAttendanceQuery.of(1L, 2L, null, null).from()).isBefore(Instant.now());
        assertThat(AdminOperationsStudyGroupsQuery.of(1L, 2L, null, null).from()).isBefore(Instant.now());
        assertThat(AdminOperationsSignupsQuery.of(1L, 2L, null, null).from()).isBefore(Instant.now());

        Instant same = Instant.parse("2026-01-01T00:00:00Z");
        assertThatThrownBy(() -> AdminOperationsOverviewQuery.of(1L, 2L, same, same))
            .isInstanceOf(AnalyticsDomainException.class);
        assertThatThrownBy(() -> AdminOperationsPointsQuery.of(1L, 2L, same, same))
            .isInstanceOf(AnalyticsDomainException.class);
        assertThatThrownBy(() -> AdminOperationsAttendanceQuery.of(1L, 2L, same, same))
            .isInstanceOf(AnalyticsDomainException.class);
        assertThatThrownBy(() -> AdminOperationsStudyGroupsQuery.of(1L, 2L, same, same))
            .isInstanceOf(AnalyticsDomainException.class);
        assertThatThrownBy(() -> AdminOperationsSignupsQuery.of(1L, 2L, same, same))
            .isInstanceOf(AnalyticsDomainException.class);
    }

    @Test
    @DisplayName("학교 정렬은 기본·허용값을 해석하고 다른 화면 정렬과 알 수 없는 값을 거부한다")
    void school_sort_is_whitelisted() {
        assertThat(AdminAnalyticsSort.schoolSummaryOf(null))
            .isEqualTo(AdminAnalyticsSort.RISK_CHALLENGER_COUNT_DESC);
        assertThat(AdminAnalyticsSort.schoolSummaryOf(" "))
            .isEqualTo(AdminAnalyticsSort.RISK_CHALLENGER_COUNT_DESC);
        for (AdminAnalyticsSort sort : AdminAnalyticsSort.values()) {
            assertThat(sort.value()).isNotBlank();
        }
        assertThat(AdminAnalyticsSort.schoolSummaryOf("schoolName,asc"))
            .isEqualTo(AdminAnalyticsSort.SCHOOL_NAME_ASC);
        assertThatThrownBy(() -> AdminAnalyticsSort.schoolSummaryOf("pointSum,asc"))
            .isInstanceOf(AnalyticsDomainException.class);
        assertThatThrownBy(() -> AdminAnalyticsSort.schoolSummaryOf("unknown"))
            .isInstanceOf(AnalyticsDomainException.class);
    }

    @Test
    @DisplayName("모든 challenger 역할을 analytics 역할로 손실 없이 변환한다")
    void maps_all_admin_role_types() {
        for (ChallengerRoleType roleType : ChallengerRoleType.values()) {
            assertThat(AdminAnalyticsRoleType.from(roleType).name()).isEqualTo(roleType.name());
        }
        AdminAnalyticsScope central = AdminAnalyticsScope.of(
            AdminAnalyticsScopeType.CENTRAL, 1L, null, null, null, ChallengerRoleType.CENTRAL_PRESIDENT
        );
        AdminAnalyticsScope school = AdminAnalyticsScope.of(
            AdminAnalyticsScopeType.SCHOOL, 1L, null, 2L, null, ChallengerRoleType.SCHOOL_PRESIDENT
        );
        assertThat(central.isCentralScope()).isTrue();
        assertThat(school.isCentralScope()).isFalse();
    }

    @Test
    @DisplayName("위험군 응답은 최근 감점의 존재·부재를 모두 보존한다")
    void risk_response_preserves_optional_latest_negative_point() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        AdminRiskChallengerInfo withPoint = AdminRiskChallengerInfo.of(
            1L, 2L, "회원", "학교", ChallengerPart.WEB, -9,
            AdminRiskChallengerInfo.LatestNegativePointInfo.of(PointType.STUDY_ABSENT, createdAt, -5)
        );
        AdminRiskChallengerInfo withoutPoint = AdminRiskChallengerInfo.of(
            2L, 3L, "회원2", "학교", ChallengerPart.IOS, -8, null
        );

        assertThat(AdminRiskChallengerResponse.from(withPoint).latestNegativePoint().score()).isEqualTo(-5);
        assertThat(AdminRiskChallengerResponse.from(withoutPoint).latestNegativePoint()).isNull();
    }

    @Test
    @DisplayName("analytics 예외는 기본 메시지와 사용자 메시지 생성자를 모두 지원한다")
    void domain_exception_supports_both_constructors() {
        assertThat(new AnalyticsDomainException(AnalyticsErrorCode.INVALID_SORT).getBaseCode())
            .isEqualTo(AnalyticsErrorCode.INVALID_SORT);
        assertThat(new AnalyticsDomainException(AnalyticsErrorCode.INVALID_SORT, "custom").getMessage())
            .contains("custom");
    }
}
