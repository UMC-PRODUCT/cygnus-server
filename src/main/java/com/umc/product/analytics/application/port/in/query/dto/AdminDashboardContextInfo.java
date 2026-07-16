package com.umc.product.analytics.application.port.in.query.dto;

import com.umc.product.analytics.domain.AdminAnalyticsRoleType;
import com.umc.product.analytics.domain.AdminAnalyticsScope;
import com.umc.product.analytics.domain.AdminAnalyticsScopeType;
import com.umc.product.common.domain.enums.ChallengerPart;

public record AdminDashboardContextInfo(
    AdminAnalyticsRoleType roleType,
    Long gisuId,
    Long chapterId,
    Long schoolId,
    ChallengerPart responsiblePart,
    AdminAnalyticsScopeType scopeType
) {

    public static AdminDashboardContextInfo from(AdminAnalyticsScope scope) {
        return new AdminDashboardContextInfo(
            scope.roleType(),
            scope.gisuId(),
            scope.chapterId(),
            scope.schoolId(),
            scope.responsiblePart(),
            scope.type()
        );
    }
}
