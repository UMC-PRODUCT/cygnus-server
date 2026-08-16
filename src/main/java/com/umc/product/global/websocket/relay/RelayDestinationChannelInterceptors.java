package com.umc.product.global.websocket.relay;

import java.util.Objects;
import java.util.function.UnaryOperator;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import com.umc.product.global.config.WebSocketBrokerProperties;

@Component
public final class RelayDestinationChannelInterceptors {

    private final ChannelInterceptor toBroker;
    private final ChannelInterceptor fromBroker;

    public RelayDestinationChannelInterceptors(
        WebSocketBrokerProperties brokerProperties,
        RelayDestinationCodec codec
    ) {
        this.toBroker = new TranslatingInterceptor(brokerProperties, codec::toBroker);
        this.fromBroker = new TranslatingInterceptor(brokerProperties, codec::toPublic);
    }

    public ChannelInterceptor toBroker() {
        return toBroker;
    }

    public ChannelInterceptor fromBroker() {
        return fromBroker;
    }

    private static final class TranslatingInterceptor implements ChannelInterceptor {

        private final WebSocketBrokerProperties brokerProperties;
        private final UnaryOperator<String> destinationTranslator;

        private TranslatingInterceptor(
            WebSocketBrokerProperties brokerProperties,
            UnaryOperator<String> destinationTranslator
        ) {
            this.brokerProperties = brokerProperties;
            this.destinationTranslator = destinationTranslator;
        }

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            if (brokerProperties.mode() != WebSocketBrokerProperties.Mode.RELAY) {
                return message;
            }

            String destination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
            String translatedDestination = destinationTranslator.apply(destination);
            if (Objects.equals(destination, translatedDestination)) {
                return message;
            }

            MessageHeaderAccessor headerAccessor = MessageHeaderAccessor.getAccessor(
                message,
                MessageHeaderAccessor.class
            );
            if (headerAccessor instanceof SimpMessageHeaderAccessor simpAccessor
                && simpAccessor.isMutable()) {
                simpAccessor.setDestination(translatedDestination);
                return message;
            }

            SimpMessageHeaderAccessor mutableAccessor = headerAccessor instanceof StompHeaderAccessor
                ? StompHeaderAccessor.wrap(message)
                : SimpMessageHeaderAccessor.wrap(message);
            mutableAccessor.setDestination(translatedDestination);
            mutableAccessor.setLeaveMutable(true);
            return MessageBuilder.createMessage(message.getPayload(), mutableAccessor.getMessageHeaders());
        }
    }
}
