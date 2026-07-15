package com.umc.product.global.websocket.adapter.out;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * {@link StompBroadcastAdapter} 단위 테스트.
 *
 * <h3>예외 전파 책임 분리</h3>
 *
 * <p>이 어댑터는 {@link SimpMessagingTemplate}의 예외를 삼키지 않고 그대로 전파한다.
 * 어댑터는 브로커 예외를 삼키지 않아 호출한 소비 도메인이 실패를 처리할 수 있게 한다.
 *
 * <p>destination 경로 조립은 호출한 소비 도메인의 책임이며,
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
    void broadcastUsesDestinationAsIs() {
        // given
        String destination = "/topic/test/rooms/10/messages";
        TestPayload payload = new TestPayload(100L, "안녕하세요");

        // when
        sut.broadcast(destination, payload);

        // then
        // 경로 조립은 호출자 책임이므로 어댑터는 인자를 그대로 전달해야 한다
        then(messagingTemplate).should().convertAndSend(destination, payload);
        then(messagingTemplate).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("브로커 예외 발생 시 예외를 삼키지 않고 그대로 전파한다")
    void broadcastPropagatesBrokerException() {
        // given
        String destination = "/topic/test/rooms/10/messages";
        TestPayload payload = new TestPayload(100L, "안녕하세요");
        willThrow(new MessagingException("브로커 연결 실패"))
            .given(messagingTemplate).convertAndSend(destination, payload);

        // when & then
        assertThatThrownBy(() -> sut.broadcast(destination, payload))
            .isInstanceOf(MessagingException.class)
            .hasMessageContaining("브로커 연결 실패");
    }

    private record TestPayload(Long id, String content) {
    }
}
