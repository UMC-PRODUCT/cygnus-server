package com.umc.product.inhouse.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductOrganizationChartInfo;

public record UmcProductOrganizationChartResponse(
    List<UmcProductChapterResponse> chapters,
    List<UmcProductDepartmentResponse> departments
) {
    public static UmcProductOrganizationChartResponse from(UmcProductOrganizationChartInfo info) {
        return new UmcProductOrganizationChartResponse(
            info.chapters().stream().map(UmcProductChapterResponse::from).toList(),
            info.departments().stream().map(UmcProductDepartmentResponse::from).toList()
        );
    }
}
