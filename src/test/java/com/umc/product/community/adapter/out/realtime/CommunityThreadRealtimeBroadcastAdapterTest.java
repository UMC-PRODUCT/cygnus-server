package com.umc.product.community.adapter.out.realtime;

import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimeEvent;
import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimeEventType;
import com.umc.product.community.application.port.in.realtime.dto.CommunityThreadRealtimePayload;
import com.umc.product.global.websocket.application.port.out.BroadcastPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("Community thread realtime destination adapter")
class CommunityThreadRealtimeBroadcastAdapterTest {

    @Mock
    BroadcastPort broadcastPort;

    @InjectMocks
    CommunityThreadRealtimeBroadcastAdapter sut;

    @Test
    @DisplayName("thread event는 공유 topic이 아닌 member별 topic으로 전달한다")
    void threadEventUsesPerMemberDestination() {
        CommunityThreadRealtimeEvent<?> event = event();

        sut.broadcastToThreadMember(11L, 20L, event);

        then(broadcastPort).should().broadcast(
            "/topic/community/threads/11/members/20/events",
            event
        );
    }

    @Test
    @DisplayName("invite event는 초대 대상의 personal topic으로 전달한다")
    void inviteEventUsesPersonalDestination() {
        CommunityThreadRealtimeEvent<?> event = event();

        sut.broadcastToMember(20L, event);

        then(broadcastPort).should().broadcast("/topic/community/members/20/events", event);
    }

    private CommunityThreadRealtimeEvent<?> event() {
        return CommunityThreadRealtimeEvent.of(
            UUID.fromString("97af680a-960e-4d37-8524-16a693f2621d"),
            CommunityThreadRealtimeEventType.READ_UPDATED,
            11L,
            Instant.parse("2026-07-18T00:00:00Z"),
            new CommunityThreadRealtimePayload.ReadUpdated(20L, 900L)
        );
    }
}
