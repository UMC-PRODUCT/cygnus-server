package com.umc.product.analytics.application.port.in.query;

import com.umc.product.analytics.application.port.in.query.dto.AdminGisuPointsInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminGisuPointsQuery;

public interface GetAdminGisuPointsUseCase {
    AdminGisuPointsInfo getGisuPoints(AdminGisuPointsQuery query);
}
