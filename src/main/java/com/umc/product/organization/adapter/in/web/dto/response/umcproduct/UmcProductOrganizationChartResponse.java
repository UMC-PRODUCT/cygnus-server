package com.umc.product.organization.adapter.in.web.dto.response.umcproduct;

import java.util.List;

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductOrganizationChartInfo;

public record UmcProductOrganizationChartResponse(
    List<UmcProductOrganizationChartChapterResponse> chapters,
    List<UmcProductSquadResponse> squads
) {
    public static UmcProductOrganizationChartResponse from(UmcProductOrganizationChartInfo info) {
        return new UmcProductOrganizationChartResponse(
            info.chapters().stream().map(UmcProductOrganizationChartChapterResponse::from).toList(),
            info.squads().stream().map(UmcProductSquadResponse::from).toList()
        );
    }
}
