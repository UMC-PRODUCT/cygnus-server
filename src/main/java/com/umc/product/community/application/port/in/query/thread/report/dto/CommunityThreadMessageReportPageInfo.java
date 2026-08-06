package com.umc.product.community.application.port.in.query.thread.report.dto;

import java.util.List;

public record CommunityThreadMessageReportPageInfo(
    List<CommunityThreadMessageAdminReportInfo> items,
    Integer nextOffset,
    long total
) {

    public CommunityThreadMessageReportPageInfo {
        items = items == null ? List.of() : List.copyOf(items);
        if (nextOffset != null && nextOffset < 0) {
            throw new IllegalArgumentException("nextOffset must not be negative");
        }
        if (total < 0) {
            throw new IllegalArgumentException("total must not be negative");
        }
    }
}
