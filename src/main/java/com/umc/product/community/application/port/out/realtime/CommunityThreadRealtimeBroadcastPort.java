package com.umc.product.community.application.port.out.realtime;

import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimeEvent;
import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimePayload;

public interface CommunityThreadRealtimeBroadcastPort {

    void broadcastToMember(
        Long memberId,
        CommunityThreadRealtimeEvent<? extends CommunityThreadRealtimePayload> event
    );
}
