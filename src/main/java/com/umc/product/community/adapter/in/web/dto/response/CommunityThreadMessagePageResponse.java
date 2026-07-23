package com.umc.product.community.adapter.in.web.dto.response;

import static com.umc.product.community.adapter.in.web.CommunityWebNumbers.text;

import java.util.List;

import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessagePageInfo;

public record CommunityThreadMessagePageResponse(
    List<CommunityThreadMessageResponse> messages,
    boolean hasMore,
    String nextBefore
) {

    public static CommunityThreadMessagePageResponse from(CommunityThreadMessagePageInfo info) {
        return new CommunityThreadMessagePageResponse(
            info.messages().stream().map(CommunityThreadMessageResponse::from).toList(),
            info.hasMore(),
            text(info.nextBefore())
        );
    }
}
