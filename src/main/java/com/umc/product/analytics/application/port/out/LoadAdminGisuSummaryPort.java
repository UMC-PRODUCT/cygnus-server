package com.umc.product.analytics.application.port.out;

import com.umc.product.analytics.application.port.in.query.dto.AdminGisuSummaryInfo;
import com.umc.product.analytics.domain.AdminAnalyticsScope;

public interface LoadAdminGisuSummaryPort {
    AdminGisuSummaryInfo getGisuSummary(AdminAnalyticsScope scope);
}
