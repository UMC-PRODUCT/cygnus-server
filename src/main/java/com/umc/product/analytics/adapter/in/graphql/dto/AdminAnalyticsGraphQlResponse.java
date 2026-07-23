package com.umc.product.analytics.adapter.in.graphql.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.umc.product.analytics.application.port.in.query.dto.AdminDashboardSummaryInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminRiskChallengerInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminSchoolSummaryInfo;
import com.umc.product.global.graphql.dto.PageInfoGraphQlResponse;

public final class AdminAnalyticsGraphQlResponse {

    private AdminAnalyticsGraphQlResponse() {
    }

    public record Root(Long requesterMemberId, AdminAnalyticsGraphQlRequest input) {
    }

    public record Summary(
        long activeChallengerCount,
        long newMemberCountThisWeek,
        double newMemberDeltaPercent,
        long activeSchoolCount,
        long activeChapterCount,
        AdminDashboardSummaryInfo.PointSumInfo monthlyPointSum,
        List<StatusCount> challengerStatusDistribution
    ) {

        public static Summary from(AdminDashboardSummaryInfo info) {
            return new Summary(
                info.activeChallengerCount(),
                info.newMemberCountThisWeek(),
                info.newMemberDeltaPercent(),
                info.activeSchoolCount(),
                info.activeChapterCount(),
                info.monthlyPointSum(),
                info.challengerStatusDistribution().entrySet().stream()
                    .map(entry -> new StatusCount(entry.getKey(), entry.getValue()))
                    .toList()
            );
        }
    }

    public record StatusCount(
        com.umc.product.common.domain.enums.ChallengerStatus status,
        long count
    ) {
    }

    public record RiskPage(
        List<AdminRiskChallengerInfo> content,
        PageInfoGraphQlResponse pageInfo
    ) {

        public static RiskPage from(Page<AdminRiskChallengerInfo> page) {
            return new RiskPage(page.getContent(), PageInfoGraphQlResponse.from(page));
        }
    }

    public record SchoolPage(
        List<AdminSchoolSummaryInfo> content,
        PageInfoGraphQlResponse pageInfo
    ) {

        public static SchoolPage from(Page<AdminSchoolSummaryInfo> page) {
            return new SchoolPage(page.getContent(), PageInfoGraphQlResponse.from(page));
        }
    }
}
