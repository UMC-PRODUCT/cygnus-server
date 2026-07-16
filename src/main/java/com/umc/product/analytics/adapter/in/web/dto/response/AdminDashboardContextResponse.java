package com.umc.product.analytics.adapter.in.web.dto.response;

import com.umc.product.analytics.application.port.in.query.dto.AdminDashboardContextInfo;
import com.umc.product.analytics.domain.AdminAnalyticsRoleType;
import com.umc.product.analytics.domain.AdminAnalyticsScopeType;
import com.umc.product.common.domain.enums.ChallengerPart;

public record AdminDashboardContextResponse(
    AdminAnalyticsRoleType roleType,
    Long gisuId,
    Long chapterId,
    Long schoolId,
    ChallengerPart responsiblePart,
    AdminAnalyticsScopeType scopeType
) {

    public static AdminDashboardContextResponse from(AdminDashboardContextInfo info) {
        return new AdminDashboardContextResponse(
            info.roleType(),
            info.gisuId(),
            info.chapterId(),
            info.schoolId(),
            info.responsiblePart(),
            info.scopeType()
        );
    }
}
