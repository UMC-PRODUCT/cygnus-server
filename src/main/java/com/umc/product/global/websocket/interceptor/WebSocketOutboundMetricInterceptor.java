package com.umc.product.global.websocket.interceptor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketOutboundMetricInterceptor implements ChannelInterceptor {

    private final MeterRegistry meterRegistry;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
        SimpMessageType messageType = accessor.getMessageType();
        String type = (messageType != null) ? messageType.name() : "UNKNOWN";

        Counter.builder("websocket.outbound.frames")
            .tag("command", type)
            .register(meterRegistry)
            .increment();

        return message;
    }
}
