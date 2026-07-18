package com.umc.product.community.adapter.in.web.dto.response;

import java.time.Instant;

import com.umc.product.community.application.port.in.query.thread.dto.ThreadLastMessageInfo;

public record CommunityThreadLastMessageResponse(
    String preview,
    String senderName,
    Instant createdAt
) {

    public static CommunityThreadLastMessageResponse from(ThreadLastMessageInfo info) {
        return info == null ? null : new CommunityThreadLastMessageResponse(
            info.preview(),
            info.senderName(),
            info.createdAt()
        );
    }
}
