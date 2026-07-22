package com.umc.product.global.websocket.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.websocket.application.service.StompClientMessageIdResolverRegistry;
import com.umc.product.global.websocket.application.service.StompSendAuthorizerRegistry;
import com.umc.product.global.websocket.application.service.StompSubscriptionAuthorizerRegistry;

@ExtendWith(MockitoExtension.class)
@DisplayName("StompAuthChannelInterceptor")
class StompAuthChannelInterceptorTest {

    @Mock
    StompSubscriptionAuthorizerRegistry subscriptionAuthorizerRegistry;

    @Mock
    StompSendAuthorizerRegistry sendAuthorizerRegistry;

    @Mock
    StompClientMessageIdResolverRegistry clientMessageIdResolverRegistry;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    StompAuthChannelInterceptor sut;

    @Test
    @DisplayName("/topic 하위 경로로 직접 SEND하는 프레임은 CommonException이 발생한다")
    void send_directly_to_broker_topic_throws() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/topic/test/rooms/10/messages");

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
    @DisplayName("Spring broker가 prefix로 인식하는 경로로 직접 SEND하면 CommonException이 발생한다")
    void send_directly_to_broker_prefix_collision_throws() {
        for (String destination : List.of("/topic-evil", "/topic**/rooms/10", "/queue-evil")) {
            Message<byte[]> message = stompMessage(StompCommand.SEND, destination);

            assertThatThrownBy(() -> sut.preSend(message, null))
                .isInstanceOf(CommonException.class)
                .extracting("baseCode")
                .isEqualTo(CommonErrorCode.SECURITY_WEBSOCKET_BROKER_ACCESS);
        }
    }

    @Test
    @DisplayName("사용자 destination으로 직접 SEND하는 프레임은 CommonException이 발생한다")
    void send_directly_to_user_destination_throws() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/user/20/queue/errors");

        assertThatThrownBy(() -> sut.preSend(message, null))
            .isInstanceOf(CommonException.class)
            .extracting("baseCode")
            .isEqualTo(CommonErrorCode.SECURITY_WEBSOCKET_BROKER_ACCESS);
    }

    @Test
    @DisplayName("server-only MESSAGE 명령을 client inbound로 보내면 destination과 무관하게 CommonException이 발생한다")
    void message_from_client_throws() {
        for (String destination : List.of(
            "/topic/test/rooms/10/messages",
            "/user/20/queue/errors",
            "/app/test/messages"
        )) {
            Message<byte[]> message = stompMessage(StompCommand.MESSAGE, destination);

            assertThatThrownBy(() -> sut.preSend(message, null))
                .isInstanceOf(CommonException.class)
                .extracting("baseCode")
                .isEqualTo(CommonErrorCode.SECURITY_WEBSOCKET_INVALID_DESTINATION);
        }
    }

    @Test
    @DisplayName("application destination으로 SEND하는 프레임은 통과된다")
    void send_to_application_destination_passes() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/app/ws-test/rooms/10/messages");

        assertThat(sut.preSend(message, null)).isSameAs(message);
    }

    @Test
    @DisplayName("소비 도메인 authorizer가 승인한 broker 토픽 SUBSCRIBE는 통과된다")
    void subscribe_to_authorized_broker_destination_passes() {
        String destination = "/topic/test/rooms/10/messages";
        given(subscriptionAuthorizerRegistry.isAuthorized(10L, destination)).willReturn(true);
        Message<byte[]> message = authenticatedStompMessage(StompCommand.SUBSCRIBE, destination, 10L);

        assertThat(sut.preSend(message, null)).isSameAs(message);
    }

    @Test
    @DisplayName("소비 도메인 authorizer가 승인하지 않은 broker 토픽 SUBSCRIBE는 거부된다")
    void subscribe_to_unauthorized_broker_destination_throws() {
        String destination = "/topic/test/rooms/10/messages";
        Message<byte[]> message = authenticatedStompMessage(StompCommand.SUBSCRIBE, destination, 10L);

        assertThatThrownBy(() -> sut.preSend(message, null))
            .isInstanceOf(CommonException.class)
            .extracting("baseCode")
            .isEqualTo(CommonErrorCode.SECURITY_WEBSOCKET_INVALID_DESTINATION);
    }

    @Test
    @DisplayName("인증 주체가 없는 broker 토픽 SUBSCRIBE는 거부된다")
    void subscribe_without_principal_throws() {
        Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, "/topic/test/rooms/10/messages");

        assertThatThrownBy(() -> sut.preSend(message, null))
            .isInstanceOf(CommonException.class)
            .extracting("baseCode")
            .isEqualTo(CommonErrorCode.SECURITY_WEBSOCKET_INVALID_DESTINATION);
    }

    @Test
    @DisplayName("Spring broker가 prefix로 인식하는 경로의 SUBSCRIBE는 authorizer 없이 거부된다")
    void subscribe_to_broker_prefix_collision_throws() {
        for (String destination : List.of("/topic-evil", "/topic**/rooms/10", "/queue-evil")) {
            Message<byte[]> message = authenticatedStompMessage(StompCommand.SUBSCRIBE, destination, 10L);

            assertThatThrownBy(() -> sut.preSend(message, null))
                .isInstanceOf(CommonException.class)
                .extracting("baseCode")
                .isEqualTo(CommonErrorCode.SECURITY_WEBSOCKET_INVALID_DESTINATION);
        }
    }

    @Test
    @DisplayName("사용자 오류 queue SUBSCRIBE는 소비 도메인 인가 대상이 아니므로 통과된다")
    void subscribe_to_user_error_queue_passes() {
        Message<byte[]> message = authenticatedStompMessage(StompCommand.SUBSCRIBE, "/user/queue/errors", 10L);

        assertThat(sut.preSend(message, null)).isSameAs(message);
    }

    @Test
    @DisplayName("소비 도메인 authorizer가 승인한 user destination SUBSCRIBE는 통과된다")
    void subscribe_to_authorized_user_destination_passes() {
        String destination = "/user/queue/community/threads/events";
        given(subscriptionAuthorizerRegistry.isAuthorized(10L, destination)).willReturn(true);
        Message<byte[]> message = authenticatedStompMessage(StompCommand.SUBSCRIBE, destination, 10L);

        assertThat(sut.preSend(message, null)).isSameAs(message);
    }

    @Test
    @DisplayName("오류 queue 외 사용자 destination SUBSCRIBE는 거부된다")
    void subscribe_to_unknown_user_destination_throws() {
        Message<byte[]> message = authenticatedStompMessage(StompCommand.SUBSCRIBE, "/user/queue/other", 10L);

        assertThatThrownBy(() -> sut.preSend(message, null))
            .isInstanceOf(CommonException.class)
            .extracting("baseCode")
            .isEqualTo(CommonErrorCode.SECURITY_WEBSOCKET_INVALID_DESTINATION);
    }

    @Test
    @DisplayName("CONNECT 프레임은 통과된다")
    void connect_passes() {
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, null);

        assertThat(sut.preSend(message, null)).isSameAs(message);
    }

    @Test
    @DisplayName("STOMP header가 없는 일반 message는 그대로 통과한다")
    void message_without_stomp_headers_passes() {
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();

        assertThat(sut.preSend(message, null)).isSameAs(message);
    }

    private Message<byte[]> stompMessage(StompCommand command, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> authenticatedStompMessage(StompCommand command, String destination, Long memberId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setUser(new UsernamePasswordAuthenticationToken(
            new MemberPrincipal(memberId),
            null,
            List.of()
        ));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
