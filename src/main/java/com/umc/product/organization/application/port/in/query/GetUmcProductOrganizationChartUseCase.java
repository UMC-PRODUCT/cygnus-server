package com.umc.product.organization.application.port.in.query;

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductOrganizationChartInfo;

public interface GetUmcProductOrganizationChartUseCase {

    UmcProductOrganizationChartInfo getCurrent();
}
