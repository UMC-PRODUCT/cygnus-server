package com.umc.product.analytics.application.port.in.query;

import com.umc.product.analytics.application.port.in.query.dto.AdminGisuSummaryInfo;
import com.umc.product.analytics.application.port.in.query.dto.AdminGisuSummaryQuery;

public interface GetAdminGisuSummaryUseCase {
    AdminGisuSummaryInfo getGisuSummary(AdminGisuSummaryQuery query);
}
