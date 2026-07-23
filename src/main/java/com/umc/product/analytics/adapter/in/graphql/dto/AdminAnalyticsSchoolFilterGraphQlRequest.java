package com.umc.product.analytics.adapter.in.graphql.dto;

import com.umc.product.analytics.domain.AdminAnalyticsSort;

public record AdminAnalyticsSchoolFilterGraphQlRequest(
    String search,
    AdminAnalyticsSort sort
) {

    public static AdminAnalyticsSchoolFilterGraphQlRequest defaultIfNull(
        AdminAnalyticsSchoolFilterGraphQlRequest filter
    ) {
        return filter == null
            ? new AdminAnalyticsSchoolFilterGraphQlRequest(
                null,
                AdminAnalyticsSort.RISK_CHALLENGER_COUNT_DESC
            )
            : filter;
    }
}
