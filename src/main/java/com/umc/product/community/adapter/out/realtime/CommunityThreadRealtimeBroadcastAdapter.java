package com.umc.product.community.adapter.out.realtime;

import org.springframework.stereotype.Component;

import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimeEvent;
import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimePayload;
import com.umc.product.community.application.port.out.realtime.CommunityThreadRealtimeBroadcastPort;
import com.umc.product.global.websocket.application.port.out.BroadcastPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CommunityThreadRealtimeBroadcastAdapter implements CommunityThreadRealtimeBroadcastPort {

    private static final String USER_DESTINATION = "/queue/community/threads/events";

    private final BroadcastPort broadcastPort;

    @Override
    public void broadcastToMember(
        Long memberId,
        CommunityThreadRealtimeEvent<? extends CommunityThreadRealtimePayload> event
    ) {
        broadcastPort.broadcastToUser(memberId.toString(), USER_DESTINATION, event);
    }
}
