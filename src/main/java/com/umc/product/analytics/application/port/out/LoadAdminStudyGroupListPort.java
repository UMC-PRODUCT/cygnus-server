package com.umc.product.analytics.application.port.out;

import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupListInfo;
import com.umc.product.analytics.domain.AdminAnalyticsScope;

public interface LoadAdminStudyGroupListPort {

    AdminStudyGroupListInfo getStudyGroupList(AdminAnalyticsScope scope);
}
