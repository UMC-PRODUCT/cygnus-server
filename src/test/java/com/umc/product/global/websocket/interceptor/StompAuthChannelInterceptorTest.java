package com.umc.product.global.websocket.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.global.exception.constant.CommonErrorCode;

@DisplayName("StompAuthChannelInterceptor")
class StompAuthChannelInterceptorTest {

    private final StompAuthChannelInterceptor sut = new StompAuthChannelInterceptor();

    @Test
    @DisplayName("/topic 하위 경로로 직접 SEND하는 프레임은 CommonException이 발생한다")
    void send_directly_to_broker_topic_throws() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/topic/chat/rooms/10/messages");

        assertThatThrownBy(() -> sut.preSend(message, null))
            .isInstanceOf(CommonException.class)
            .extracting("baseCode")
            .isEqualTo(CommonErrorCode.SECURITY_WEBSOCKET_BROKER_ACCESS);
    }

    @Test
    @DisplayName("/topic 경로로 직접 SEND하는 프레임은 CommonException이 발생한다")
    void send_directly_to_exact_broker_topic_throws() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/topic");

        assertThatThrownBy(() -> sut.preSend(message, null))
            .isInstanceOf(CommonException.class)
            .extracting("baseCode")
            .isEqualTo(CommonErrorCode.SECURITY_WEBSOCKET_BROKER_ACCESS);
    }

    @Test
    @DisplayName("/queue 하위 경로로 직접 SEND하는 프레임은 CommonException이 발생한다")
    void send_directly_to_broker_queue_throws() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/queue/errors");

        assertThatThrownBy(() -> sut.preSend(message, null))
            .isInstanceOf(CommonException.class)
            .extracting("baseCode")
            .isEqualTo(CommonErrorCode.SECURITY_WEBSOCKET_BROKER_ACCESS);
    }

    @Test
    @DisplayName("/queue 경로로 직접 SEND하는 프레임은 CommonException이 발생한다")
    void send_directly_to_exact_broker_queue_throws() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/queue");

        assertThatThrownBy(() -> sut.preSend(message, null))
            .isInstanceOf(CommonException.class)
            .extracting("baseCode")
            .isEqualTo(CommonErrorCode.SECURITY_WEBSOCKET_BROKER_ACCESS);
    }

    @Test
    @DisplayName("application destination으로 SEND하는 프레임은 통과된다")
    void send_to_application_destination_passes() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/app/ws-test/rooms/10/messages");

        assertThat(sut.preSend(message, null)).isSameAs(message);
    }

    @Test
    @DisplayName("broker destination을 SUBSCRIBE하는 프레임은 통과된다")
    void subscribe_to_broker_destination_passes() {
        Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, "/topic/ws-test/rooms/10/messages");

        assertThat(sut.preSend(message, null)).isSameAs(message);
    }

    @Test
    @DisplayName("CONNECT 프레임은 통과된다")
    void connect_passes() {
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, null);

        assertThat(sut.preSend(message, null)).isSameAs(message);
    }

    private Message<byte[]> stompMessage(StompCommand command, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
