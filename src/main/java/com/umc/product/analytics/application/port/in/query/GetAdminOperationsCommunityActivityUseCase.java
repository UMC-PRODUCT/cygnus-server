package com.umc.product.analytics.application.port.in.query;

import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsCommunityActivityInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminOperationsCommunityActivityQuery;

public interface GetAdminOperationsCommunityActivityUseCase {

    AdminOperationsCommunityActivityInfo getCommunityActivity(AdminOperationsCommunityActivityQuery query);
}
