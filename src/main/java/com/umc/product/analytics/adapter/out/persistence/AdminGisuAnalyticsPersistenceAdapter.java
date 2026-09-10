package com.umc.product.analytics.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.umc.product.analytics.application.port.in.query.dto.AdminGisuPointsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminGisuSummaryInfo;
import com.umc.product.analytics.application.port.out.LoadAdminGisuPointsPort;
import com.umc.product.analytics.application.port.out.LoadAdminGisuSummaryPort;
import com.umc.product.analytics.domain.AdminAnalyticsScope;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminGisuAnalyticsPersistenceAdapter implements LoadAdminGisuSummaryPort, LoadAdminGisuPointsPort {

    private final AdminGisuAnalyticsQueryRepository queryRepository;

    @Override
    public AdminGisuSummaryInfo getGisuSummary(AdminAnalyticsScope scope) {
        return queryRepository.getGisuSummary(scope);
    }

    @Override
    public AdminGisuPointsInfo getGisuPoints(AdminAnalyticsScope scope) {
        return queryRepository.getGisuPoints(scope);
    }
}
