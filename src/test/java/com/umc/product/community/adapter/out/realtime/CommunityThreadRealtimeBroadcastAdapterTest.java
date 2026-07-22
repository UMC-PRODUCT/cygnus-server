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
    @DisplayName("모든 정상 event는 대상 member의 Community thread user queue로 전달한다")
    void eventUsesCommunityThreadUserDestination() {
        CommunityThreadRealtimeEvent<?> event = event();

        sut.broadcastToMember(20L, event);

        then(broadcastPort).should().broadcastToUser(
            "20",
            "/queue/community/threads/events",
            event
        );
        then(broadcastPort).shouldHaveNoMoreInteractions();
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
