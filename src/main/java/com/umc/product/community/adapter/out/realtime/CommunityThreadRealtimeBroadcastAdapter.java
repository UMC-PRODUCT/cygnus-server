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

    private static final String THREAD_MEMBER_DESTINATION =
        "/topic/community/threads/%d/members/%d/events";
    private static final String MEMBER_DESTINATION = "/topic/community/members/%d/events";

    private final BroadcastPort broadcastPort;

    @Override
    public void broadcastToThreadMember(
        Long threadId,
        Long memberId,
        CommunityThreadRealtimeEvent<? extends CommunityThreadRealtimePayload> event
    ) {
        broadcastPort.broadcast(THREAD_MEMBER_DESTINATION.formatted(threadId, memberId), event);
    }

    @Override
    public void broadcastToMember(
        Long memberId,
        CommunityThreadRealtimeEvent<? extends CommunityThreadRealtimePayload> event
    ) {
        broadcastPort.broadcast(MEMBER_DESTINATION.formatted(memberId), event);
    }
}
