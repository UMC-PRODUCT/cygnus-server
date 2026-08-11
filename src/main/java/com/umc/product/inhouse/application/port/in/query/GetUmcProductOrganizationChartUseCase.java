package com.umc.product.inhouse.application.port.in.query;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductOrganizationChartInfo;

public interface GetUmcProductOrganizationChartUseCase {

    UmcProductOrganizationChartInfo getCurrent();
}
