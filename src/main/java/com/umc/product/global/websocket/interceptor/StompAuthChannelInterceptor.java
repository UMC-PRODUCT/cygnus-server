package com.umc.product.global.websocket.interceptor;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.global.exception.constant.CommonErrorCode;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    // 클라이언트가 직접 SEND할 수 없는 브로커 경로
    private static final String BROKER_TOPIC_PREFIX = "/topic";
    private static final String BROKER_QUEUE_PREFIX = "/queue";

    /**
     * 클라이언트가 broker destination으로 직접 메시지를 보내는 것을 차단한다.
     * 도메인별 SEND/SUBSCRIBE 인가는 각 소비 도메인의 WebSocket 진입점에서 처리한다.
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.SEND.equals(accessor.getCommand()) && isBrokerDestination(accessor.getDestination())) {
            throw new CommonException(CommonErrorCode.SECURITY_WEBSOCKET_BROKER_ACCESS);
        }

        return message;
    }

    /**
     * 클라이언트가 직접 SEND할 수 없는 브로커 경로인지 확인한다.
     */
    private boolean isBrokerDestination(String destination) {
        return destination != null
            && (hasDestinationPrefix(destination, BROKER_TOPIC_PREFIX)
            || hasDestinationPrefix(destination, BROKER_QUEUE_PREFIX));
    }

    private boolean hasDestinationPrefix(String destination, String prefix) {
        return destination.equals(prefix) || destination.startsWith(prefix + "/");
    }
}
