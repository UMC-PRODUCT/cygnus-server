package com.umc.product.organization.adapter.in.web.dto.response.umcproduct;

import java.util.List;

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductOrganizationChartChapterInfo;

public record UmcProductOrganizationChartChapterResponse(
    UmcProductChapterResponse chapter,
    List<UmcProductPartResponse> parts
) {
    public static UmcProductOrganizationChartChapterResponse from(UmcProductOrganizationChartChapterInfo info) {
        return new UmcProductOrganizationChartChapterResponse(
            UmcProductChapterResponse.from(info.chapter()),
            info.parts().stream().map(UmcProductPartResponse::from).toList()
        );
    }
}
