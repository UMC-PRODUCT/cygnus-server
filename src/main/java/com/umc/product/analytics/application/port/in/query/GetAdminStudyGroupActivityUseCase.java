package com.umc.product.analytics.application.port.in.query;

import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupActivityInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupActivityQuery;

public interface GetAdminStudyGroupActivityUseCase {

    AdminStudyGroupActivityInfo getStudyGroupActivity(AdminStudyGroupActivityQuery query);
}
