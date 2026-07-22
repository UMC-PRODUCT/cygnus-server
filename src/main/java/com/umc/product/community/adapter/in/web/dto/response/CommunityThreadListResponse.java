package com.umc.product.community.adapter.in.web.dto.response;

import static com.umc.product.community.adapter.in.web.CommunityWebNumbers.text;

import java.util.List;

import com.umc.product.community.application.port.in.query.thread.dto.ThreadListInfo;

public record CommunityThreadListResponse(
    List<CommunityThreadSummaryResponse> pinned,
    List<CommunityThreadSummaryResponse> threads,
    String nextOffset,
    String total
) {

    public static CommunityThreadListResponse from(ThreadListInfo info) {
        return new CommunityThreadListResponse(
            info.pinned().stream().map(CommunityThreadSummaryResponse::from).toList(),
            info.threads().stream().map(CommunityThreadSummaryResponse::from).toList(),
            text(info.nextOffset()),
            text(info.total())
        );
    }
}
