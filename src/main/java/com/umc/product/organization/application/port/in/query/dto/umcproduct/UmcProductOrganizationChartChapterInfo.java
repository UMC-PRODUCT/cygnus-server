package com.umc.product.organization.application.port.in.query.dto.umcproduct;

import java.util.List;

public record UmcProductOrganizationChartChapterInfo(
    UmcProductChapterInfo chapter,
    List<UmcProductPartInfo> parts
) {
}
