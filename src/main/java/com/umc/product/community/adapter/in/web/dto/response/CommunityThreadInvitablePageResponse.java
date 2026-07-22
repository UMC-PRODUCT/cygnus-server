package com.umc.product.community.adapter.in.web.dto.response;

import static com.umc.product.community.adapter.in.web.CommunityWebNumbers.text;

import java.util.List;

import com.umc.product.community.application.port.in.query.thread.dto.ThreadInvitablePageInfo;

public record CommunityThreadInvitablePageResponse(
    List<CommunityThreadInvitableResponse> items,
    String nextOffset,
    String total
) {

    public static CommunityThreadInvitablePageResponse from(ThreadInvitablePageInfo info) {
        return new CommunityThreadInvitablePageResponse(
            info.items().stream().map(CommunityThreadInvitableResponse::from).toList(),
            text(info.nextOffset()),
            text(info.total())
        );
    }
}
