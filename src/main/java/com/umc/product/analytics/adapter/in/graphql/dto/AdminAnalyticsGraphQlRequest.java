package com.umc.product.analytics.adapter.in.graphql.dto;

import com.umc.product.analytics.application.port.in.query.dto.AdminDashboardActionQueueQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminDashboardQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminRiskChallengerQuery;
import com.umc.product.analytics.application.port.in.query.dto.AdminSchoolSummaryQuery;
import com.umc.product.global.graphql.dto.PageGraphQlRequest;

public record AdminAnalyticsGraphQlRequest(
    Long gisuId,
    Long chapterId,
    Long schoolId,
    Integer riskThreshold
) {

    public static AdminAnalyticsGraphQlRequest defaultIfNull(AdminAnalyticsGraphQlRequest request) {
        return request == null ? new AdminAnalyticsGraphQlRequest(null, null, null, -8) : request;
    }

    public AdminDashboardQuery toDashboardQuery(Long requesterMemberId) {
        return AdminDashboardQuery.of(requesterMemberId, gisuId, chapterId, schoolId);
    }

    public AdminDashboardActionQueueQuery toActionQueueQuery(Long requesterMemberId) {
        return AdminDashboardActionQueueQuery.of(requesterMemberId, gisuId, riskThreshold);
    }

    public AdminRiskChallengerQuery toRiskQuery(Long requesterMemberId, PageGraphQlRequest page) {
        return AdminRiskChallengerQuery.of(
            requesterMemberId,
            gisuId,
            chapterId,
            schoolId,
            riskThreshold,
            PageGraphQlRequest.defaultIfNull(page).toPageableWithMaxOffset(10_000L)
        );
    }

    public AdminSchoolSummaryQuery toSchoolQuery(
        Long requesterMemberId,
        AdminAnalyticsSchoolFilterGraphQlRequest filter,
        PageGraphQlRequest page
    ) {
        AdminAnalyticsSchoolFilterGraphQlRequest normalized =
            AdminAnalyticsSchoolFilterGraphQlRequest.defaultIfNull(filter);
        return AdminSchoolSummaryQuery.of(
            requesterMemberId,
            gisuId,
            chapterId,
            normalized.search(),
            riskThreshold,
            PageGraphQlRequest.defaultIfNull(page).toPageableWithMaxOffset(10_000L),
            normalized.sort().value()
        );
    }
}
