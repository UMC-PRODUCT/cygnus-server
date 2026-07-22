package com.umc.product.community.adapter.in.websocket;

import java.util.UUID;

record CommunityStompCommandContext(Long memberId, String userName, UUID commandId) {

    CommunityStompCorrelation correlation(
        CommunityStompCommandType command,
        UUID clientMessageId
    ) {
        return new CommunityStompCorrelation(userName, commandId, clientMessageId, command);
    }
}
