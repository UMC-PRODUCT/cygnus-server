package com.umc.product.analytics.application.port.in.query;

import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupListInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminStudyGroupListQuery;

public interface GetAdminStudyGroupListUseCase {

    AdminStudyGroupListInfo getStudyGroupList(AdminStudyGroupListQuery query);
}
