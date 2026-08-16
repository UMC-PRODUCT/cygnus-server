package com.umc.product.community.adapter.in.web.dto.response;

import static com.umc.product.community.adapter.in.web.CommunityWebNumbers.text;

import java.util.List;

import com.umc.product.community.application.port.in.query.thread.report.dto.CommunityThreadMessageReportPageInfo;

public record CommunityThreadMessageReportPageResponse(
    List<CommunityThreadMessageAdminReportResponse> items,
    String nextOffset,
    String total
) {

    public static CommunityThreadMessageReportPageResponse from(
        CommunityThreadMessageReportPageInfo info
    ) {
        return new CommunityThreadMessageReportPageResponse(
            info.items().stream().map(CommunityThreadMessageAdminReportResponse::from).toList(),
            text(info.nextOffset()),
            text(info.total())
        );
    }
}
