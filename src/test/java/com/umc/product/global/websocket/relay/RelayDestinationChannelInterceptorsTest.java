package com.umc.product.global.websocket.relay;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import com.umc.product.global.config.WebSocketBrokerProperties;

@DisplayName("RelayDestinationChannelInterceptors")
class RelayDestinationChannelInterceptorsTest {

    private static final String PUBLIC_USER_DESTINATION =
        "/topic/__internal/user-destination";
    private static final String PUBLIC_USER_REGISTRY =
        "/topic/__internal/user-registry";
    private static final MessageChannel UNUSED_CHANNEL = (message, timeout) -> true;

    private final RelayDestinationCodec codec = new RelayDestinationCodec();

    @Test
    @DisplayName("relay SUBSCRIBE는 user system 경로를 broker key로 바꾸고 header와 payload를 보존한다")
    void translateRelaySubscribeToBroker() {
        byte[] payload = new byte[]{1, 2, 3};
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(PUBLIC_USER_DESTINATION);
        accessor.setSubscriptionId("subscription-7");
        accessor.setReceipt("receipt-9");
        accessor.setNativeHeader("x-correlation-id", "correlation-11");
        Message<byte[]> message = MessageBuilder.createMessage(payload, accessor.getMessageHeaders());

        Message<?> translated = relayInterceptors().toBroker().preSend(message, UNUSED_CHANNEL);
        StompHeaderAccessor translatedAccessor = StompHeaderAccessor.wrap(translated);

        assertThat(translatedAccessor.getDestination())
            .isEqualTo(codec.toBroker(PUBLIC_USER_DESTINATION));
        assertThat(translatedAccessor.getFirstNativeHeader("destination"))
            .isEqualTo(codec.toBroker(PUBLIC_USER_DESTINATION));
        assertThat(translatedAccessor.getReceipt()).isEqualTo("receipt-9");
        assertThat(translatedAccessor.getSubscriptionId()).isEqualTo("subscription-7");
        assertThat(translatedAccessor.getFirstNativeHeader("id")).isEqualTo("subscription-7");
        assertThat(translatedAccessor.getFirstNativeHeader("x-correlation-id"))
            .isEqualTo("correlation-11");
        assertThat(translated.getPayload()).isSameAs(payload);
    }

    @Test
    @DisplayName("application broker SEND는 user registry 경로를 broker key로 바꾸고 header와 payload를 보존한다")
    void translateApplicationSendToBroker() {
        Object payload = new Object();
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setDestination(PUBLIC_USER_REGISTRY);
        accessor.setNativeHeader("x-event-type", "INVITED");
        Message<Object> message = MessageBuilder.createMessage(payload, accessor.getMessageHeaders());

        Message<?> translated = relayInterceptors().toBroker().preSend(message, UNUSED_CHANNEL);
        StompHeaderAccessor translatedAccessor = StompHeaderAccessor.wrap(translated);

        assertThat(translatedAccessor.getDestination())
            .isEqualTo(codec.toBroker(PUBLIC_USER_REGISTRY));
        assertThat(translatedAccessor.getFirstNativeHeader("destination"))
            .isEqualTo(codec.toBroker(PUBLIC_USER_REGISTRY));
        assertThat(translatedAccessor.getFirstNativeHeader("x-event-type")).isEqualTo("INVITED");
        assertThat(translated.getPayload()).isSameAs(payload);
        assertThat(MessageHeaderAccessor.getAccessor(translated, MessageHeaderAccessor.class))
            .isExactlyInstanceOf(SimpMessageHeaderAccessor.class);
    }

    @Test
    @DisplayName("broker MESSAGE는 user system 공개 경로를 복원하고 header와 payload를 보존한다")
    void restoreBrokerMessageForClient() {
        byte[] payload = new byte[]{4, 5, 6};
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
        accessor.setDestination(codec.toBroker(PUBLIC_USER_DESTINATION));
        accessor.setSubscriptionId("subscription-17");
        accessor.setMessageId("message-19");
        accessor.setNativeHeader("x-correlation-id", "correlation-23");
        Message<byte[]> message = MessageBuilder.createMessage(payload, accessor.getMessageHeaders());

        Message<?> translated = relayInterceptors().fromBroker().preSend(message, UNUSED_CHANNEL);
        StompHeaderAccessor translatedAccessor = StompHeaderAccessor.wrap(translated);

        assertThat(translatedAccessor.getDestination()).isEqualTo(PUBLIC_USER_DESTINATION);
        assertThat(translatedAccessor.getFirstNativeHeader("destination"))
            .isEqualTo(PUBLIC_USER_DESTINATION);
        assertThat(translatedAccessor.getSubscriptionId()).isEqualTo("subscription-17");
        assertThat(translatedAccessor.getFirstNativeHeader("subscription"))
            .isEqualTo("subscription-17");
        assertThat(translatedAccessor.getMessageId()).isEqualTo("message-19");
        assertThat(translatedAccessor.getFirstNativeHeader("x-correlation-id"))
            .isEqualTo("correlation-23");
        assertThat(translated.getPayload()).isSameAs(payload);
    }

    @Test
    @DisplayName("relay mode에서 해석된 Community user session queue를 broker key로 가역 변환한다")
    void translateResolvedCommunityUserQueue() {
        String publicDestination = "/queue/community/threads/events-usersession-7";
        Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, publicDestination);

        Message<?> translated = relayInterceptors().toBroker().preSend(message, UNUSED_CHANNEL);

        assertThat(SimpMessageHeaderAccessor.getDestination(translated.getHeaders()))
            .isEqualTo("/queue/community.threads.events-usersession-7");
        Message<?> restored = relayInterceptors().fromBroker().preSend(translated, UNUSED_CHANNEL);
        assertThat(SimpMessageHeaderAccessor.getDestination(restored.getHeaders()))
            .isEqualTo(publicDestination);
    }

    @Test
    @DisplayName("relay mode에서도 소유하지 않은 destination message는 동일 instance로 통과시킨다")
    void leaveUnownedDestinationMessageUnchanged() {
        Message<byte[]> message = stompMessage(
            StompCommand.SUBSCRIBE,
            "/topic/community/unowned/path"
        );

        assertThat(relayInterceptors().toBroker().preSend(message, UNUSED_CHANNEL))
            .isSameAs(message);
        assertThat(relayInterceptors().fromBroker().preSend(message, UNUSED_CHANNEL))
            .isSameAs(message);
    }

    @ParameterizedTest
    @EnumSource(
        value = StompCommand.class,
        names = {"CONNECT", "RECEIPT", "ERROR", "DISCONNECT"}
    )
    @DisplayName("destination 없는 control frame은 동일 instance로 통과시킨다")
    void leaveDestinationlessFrameUnchanged(StompCommand command) {
        Message<byte[]> message = stompMessage(command, null);

        assertThat(relayInterceptors().toBroker().preSend(message, UNUSED_CHANNEL))
            .isSameAs(message);
        assertThat(relayInterceptors().fromBroker().preSend(message, UNUSED_CHANNEL))
            .isSameAs(message);
    }

    @Test
    @DisplayName("simple mode는 공개 및 internal destination message를 byte-for-byte 동일하게 통과시킨다")
    void leaveSimpleModeMessageUnchanged() {
        RelayDestinationChannelInterceptors simpleInterceptors = interceptors(
            WebSocketBrokerProperties.Mode.SIMPLE
        );
        Message<byte[]> publicMessage = stompMessage(
            StompCommand.SUBSCRIBE,
            PUBLIC_USER_DESTINATION
        );
        Message<byte[]> internalMessage = stompMessage(
            StompCommand.MESSAGE,
            codec.toBroker(PUBLIC_USER_DESTINATION)
        );

        assertThat(simpleInterceptors.toBroker().preSend(publicMessage, UNUSED_CHANNEL))
            .isSameAs(publicMessage);
        assertThat(simpleInterceptors.fromBroker().preSend(internalMessage, UNUSED_CHANNEL))
            .isSameAs(internalMessage);
    }

    private RelayDestinationChannelInterceptors relayInterceptors() {
        return interceptors(WebSocketBrokerProperties.Mode.RELAY);
    }

    private RelayDestinationChannelInterceptors interceptors(
        WebSocketBrokerProperties.Mode mode
    ) {
        return new RelayDestinationChannelInterceptors(
            new WebSocketBrokerProperties(mode, null),
            codec
        );
    }

    private Message<byte[]> stompMessage(StompCommand command, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        return MessageBuilder.createMessage(new byte[]{7, 8, 9}, accessor.getMessageHeaders());
    }
}
