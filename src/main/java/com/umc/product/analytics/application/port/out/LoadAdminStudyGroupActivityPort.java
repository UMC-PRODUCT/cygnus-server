package com.umc.product.analytics.application.port.out;

import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupActivityInfo;
import com.umc.product.analytics.domain.AdminAnalyticsScope;

public interface LoadAdminStudyGroupActivityPort {

    AdminStudyGroupActivityInfo getStudyGroupActivity(AdminAnalyticsScope scope);
}
