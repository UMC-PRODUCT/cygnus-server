package com.umc.product.community.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.community.adapter.in.websocket.dto.event.CommunityCommandAcknowledgement;
import com.umc.product.community.adapter.in.websocket.dto.event.CommunityStompEventEnvelope;
import com.umc.product.global.websocket.application.port.out.BroadcastPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("Community STOMP ACK publisher")
class CommunityStompAckPublisherTest {

    @Mock
    BroadcastPort broadcastPort;

    @InjectMocks
    CommunityStompAckPublisher sut;

    @Test
    @DisplayName("ACK는 command를 보낸 회원의 Community thread user queue로 전달한다")
    void publishesAckToCommunityThreadUserDestination() {
        CommunityCommandAcknowledgement acknowledgement = acknowledgement();
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);

        sut.publish(11L, 20L, acknowledgement);

        then(broadcastPort).should().broadcastToUser(
            org.mockito.ArgumentMatchers.eq("20"),
            org.mockito.ArgumentMatchers.eq("/queue/community/threads/events"),
            eventCaptor.capture()
        );
        CommunityStompEventEnvelope<?> event =
            (CommunityStompEventEnvelope<?>) eventCaptor.getValue();
        assertThat(event.type()).isEqualTo("command.acknowledged");
        assertThat(event.threadId()).isEqualTo("11");
        assertThat(event.payload()).isSameAs(acknowledgement);
    }

    @Test
    @DisplayName("ACK user destination 전송 실패는 command 성공을 되돌리지 않는다")
    void ignoresAckBroadcastFailure() {
        willThrow(new IllegalStateException("broker unavailable"))
            .given(broadcastPort).broadcastToUser(
                org.mockito.ArgumentMatchers.eq("20"),
                org.mockito.ArgumentMatchers.eq("/queue/community/threads/events"),
                any()
            );

        assertThatCode(() -> sut.publish(11L, 20L, acknowledgement()))
            .doesNotThrowAnyException();
    }

    private CommunityCommandAcknowledgement acknowledgement() {
        return new CommunityCommandAcknowledgement(
            UUID.fromString("b108f0c7-e244-4c9d-a57a-6e5bb8e94e89"),
            CommunityStompCommandType.MESSAGE_CREATE,
            900L,
            UUID.fromString("0dce06f4-11bc-4dc2-b9fd-4f9cb88ea9cd"),
            false
        );
    }
}
