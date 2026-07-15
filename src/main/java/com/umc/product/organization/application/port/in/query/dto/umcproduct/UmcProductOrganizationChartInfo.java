package com.umc.product.organization.application.port.in.query.dto.umcproduct;

import java.util.List;

public record UmcProductOrganizationChartInfo(
    List<UmcProductChapterInfo> chapters,
    List<UmcProductSquadInfo> squads
) {
}
