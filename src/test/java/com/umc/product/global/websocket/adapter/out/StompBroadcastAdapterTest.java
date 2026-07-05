package com.umc.product.global.websocket.adapter.out;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.umc.product.chat.application.port.in.command.dto.BroadcastChatMessagePayload;
import com.umc.product.chat.domain.MessageContentType;

/**
 * {@link StompBroadcastAdapter} 단위 테스트.
 *
 * <h3>예외 전파 책임 분리</h3>
 *
 * <p>이 어댑터는 {@link SimpMessagingTemplate}의 예외를 삼키지 않고 그대로 전파한다.
 * 예외 삼킴 책임은 호출 계층인 {@code ChatMessageBroadcastListener}에 있으며,
 * 어댑터가 삼기면 브로커 장애를 상위에서 인지할 수단이 사라진다.
 *
 * <p>destination 경로 조립은 호출자({@code BroadcastChatMessageService})의 책임이며,
 * 이 어댑터는 전달받은 destination을 가공 없이 그대로 전송한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StompBroadcastAdapter")
class StompBroadcastAdapterTest {

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    StompBroadcastAdapter sut;

    @Test
    @DisplayName("전달받은 destination을 가공하지 않고 그대로 전송한다")
    void broadcast_destination을_가공하지_않고_그대로_전송한다() {
        // given
        String destination = "/topic/chat/rooms/10/messages";
        BroadcastChatMessagePayload payload = new BroadcastChatMessagePayload(
            100L, 10L, 5L,
            MessageContentType.TEXT,
            "안녕하세요",
            List.of(),
            Instant.parse("2026-07-03T07:00:00Z")
        );

        // when
        sut.broadcast(destination, payload);

        // then
        // 경로 조립은 호출자 책임이므로 어댑터는 인자를 그대로 전달해야 한다
        then(messagingTemplate).should().convertAndSend(destination, payload);
        then(messagingTemplate).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("브로커 예외 발생 시 예외를 삼키지 않고 그대로 전파한다")
    void broadcast_브로커_예외_발생_시_예외를_삼키지_않고_그대로_전파한다() {
        // given
        String destination = "/topic/chat/rooms/10/messages";
        BroadcastChatMessagePayload payload = new BroadcastChatMessagePayload(
            100L, 10L, 5L,
            MessageContentType.TEXT,
            "안녕하세요",
            List.of(),
            Instant.parse("2026-07-03T07:00:00Z")
        );
        willThrow(new MessagingException("브로커 연결 실패"))
            .given(messagingTemplate).convertAndSend(destination, payload);

        // when & then
        // 예외 삼킴 책임은 ChatMessageBroadcastListener에 있다.
        // 이 어댑터가 삼키면 브로커 장애를 상위에서 인지할 수단이 사라진다.
        assertThatThrownBy(() -> sut.broadcast(destination, payload))
            .isInstanceOf(MessagingException.class)
            .hasMessageContaining("브로커 연결 실패");
    }
}
