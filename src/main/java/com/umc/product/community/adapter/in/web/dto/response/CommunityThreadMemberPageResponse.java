package com.umc.product.community.adapter.in.web.dto.response;

import static com.umc.product.community.adapter.in.web.CommunityWebNumbers.text;

import java.util.List;

import com.umc.product.community.application.port.in.query.thread.dto.ThreadMemberPageInfo;

public record CommunityThreadMemberPageResponse(
    List<CommunityThreadMemberResponse> items,
    String nextOffset,
    String total
) {

    public static CommunityThreadMemberPageResponse from(ThreadMemberPageInfo info) {
        return new CommunityThreadMemberPageResponse(
            info.items().stream().map(CommunityThreadMemberResponse::from).toList(),
            text(info.nextOffset()),
            text(info.total())
        );
    }
}
