package com.umc.product.global.websocket.interceptor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.EnumMap;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
public class WebSocketInboundMetricInterceptor implements ChannelInterceptor {

    private final EnumMap<SimpMessageType, Counter> counters;
    private final Counter unknownCounter;

    public WebSocketInboundMetricInterceptor(MeterRegistry meterRegistry) {
        this.counters = new EnumMap<>(SimpMessageType.class);
        for (SimpMessageType type : SimpMessageType.values()) {
            counters.put(type, Counter.builder("websocket.inbound.frames")
                .tag("command", type.name())
                .register(meterRegistry));
        }
        this.unknownCounter = Counter.builder("websocket.inbound.frames")
            .tag("command", "UNKNOWN")
            .register(meterRegistry);
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        SimpMessageType messageType = SimpMessageHeaderAccessor.wrap(message).getMessageType();
        (messageType != null ? counters.get(messageType) : unknownCounter).increment();
        return message;
    }
}
