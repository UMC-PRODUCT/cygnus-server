package com.umc.product.analytics.application.port.out;

import java.time.Instant;

import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsCommunityActivityInfo;
import com.umc.product.analytics.application.port.in.query.dto.Granularity;
import com.umc.product.analytics.domain.AdminAnalyticsScope;

public interface LoadAdminOperationsCommunityActivityPort {

    AdminOperationsCommunityActivityInfo getCommunityActivity(
        AdminAnalyticsScope scope,
        Instant from,
        Instant to,
        Granularity granularity
    );
}
