package com.umc.product.analytics.domain;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;

public record AdminAnalyticsScope(
    AdminAnalyticsScopeType type,
    Long gisuId,
    Long chapterId,
    Long schoolId,
    ChallengerPart responsiblePart,
    AdminAnalyticsRoleType roleType
) {

    public static AdminAnalyticsScope of(
        AdminAnalyticsScopeType type,
        Long gisuId,
        Long chapterId,
        Long schoolId,
        ChallengerPart responsiblePart,
        ChallengerRoleType roleType
    ) {
        return new AdminAnalyticsScope(
            type,
            gisuId,
            chapterId,
            schoolId,
            responsiblePart,
            AdminAnalyticsRoleType.from(roleType)
        );
    }

    public static AdminAnalyticsScope superAdmin(
        Long gisuId,
        Long chapterId,
        Long schoolId,
        ChallengerPart responsiblePart
    ) {
        return new AdminAnalyticsScope(
            AdminAnalyticsScopeType.CENTRAL,
            gisuId,
            chapterId,
            schoolId,
            responsiblePart,
            AdminAnalyticsRoleType.SUPER_ADMIN
        );
    }

    public boolean isCentralScope() {
        return type == AdminAnalyticsScopeType.CENTRAL;
    }
}
