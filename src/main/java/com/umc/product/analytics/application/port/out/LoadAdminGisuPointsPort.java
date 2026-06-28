package com.umc.product.analytics.application.port.out;

import com.umc.product.analytics.application.port.in.query.dto.AdminGisuPointsInfo;
import com.umc.product.analytics.domain.AdminAnalyticsScope;

public interface LoadAdminGisuPointsPort {
    AdminGisuPointsInfo getGisuPoints(AdminAnalyticsScope scope);
}
