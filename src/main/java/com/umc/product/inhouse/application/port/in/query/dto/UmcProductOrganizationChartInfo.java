package com.umc.product.inhouse.application.port.in.query.dto;

import java.util.List;

public record UmcProductOrganizationChartInfo(
    List<UmcProductChapterInfo> chapters,
    List<UmcProductSquadInfo> squads
) {
}
