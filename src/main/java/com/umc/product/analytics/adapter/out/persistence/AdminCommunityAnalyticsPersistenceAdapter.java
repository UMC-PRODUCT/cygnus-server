package com.umc.product.analytics.adapter.out.persistence;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsCommunityActivityInfo;
import com.umc.product.analytics.application.port.in.query.dto.Granularity;
import com.umc.product.analytics.application.port.out.LoadAdminOperationsCommunityActivityPort;
import com.umc.product.analytics.domain.AdminAnalyticsScope;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminCommunityAnalyticsPersistenceAdapter implements LoadAdminOperationsCommunityActivityPort {

    private final AdminCommunityAnalyticsQueryRepository queryRepository;

    @Override
    public AdminOperationsCommunityActivityInfo getCommunityActivity(
        AdminAnalyticsScope scope,
        Instant from,
        Instant to,
        Granularity granularity
    ) {
        return queryRepository.getCommunityActivity(scope, from, to, granularity);
    }
}
