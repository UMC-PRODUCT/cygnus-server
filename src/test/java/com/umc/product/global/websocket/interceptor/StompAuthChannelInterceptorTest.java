package com.umc.product.global.websocket.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.chat.application.port.in.query.CheckChatRoomAccessUseCase;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.websocket.handler.WebSocketErrorEvent;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@DisplayName("StompAuthChannelInterceptor")
@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    @Mock
    private CheckChatRoomAccessUseCase checkChatRoomAccessUseCase;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private MessageChannel channel;

    @InjectMocks
    private StompAuthChannelInterceptor sut;

    @Test
    @DisplayName("채팅방 접근 권한이 있는 멤버의 SUBSCRIBE 프레임은 통과된다")
    void subscribe_with_access_passes() {
        when(checkChatRoomAccessUseCase.hasChatRoomAccess(1L, 10L)).thenReturn(true);
        Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, "/topic/chat/rooms/10/messages", 1L);

        Message<?> result = sut.preSend(message, channel);

        assertThat(result).isSameAs(message);
        verify(checkChatRoomAccessUseCase).hasChatRoomAccess(1L, 10L);
    }

    @Test
    @DisplayName("채팅방 접근 권한이 있는 멤버의 SEND 프레임은 통과된다")
    void send_with_access_passes() {
        when(checkChatRoomAccessUseCase.hasChatRoomAccess(1L, 10L)).thenReturn(true);
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/app/chat/rooms/10/messages", 1L);

        Message<?> result = sut.preSend(message, channel);

        assertThat(result).isSameAs(message);
        verify(checkChatRoomAccessUseCase).hasChatRoomAccess(1L, 10L);
    }

    @Test
    @DisplayName("채팅방 접근 권한이 없는 멤버의 SEND 프레임은 에러 메시지를 전송하고 차단된다")
    void send_to_app_without_access_publishes_error_and_blocks() {
        when(checkChatRoomAccessUseCase.hasChatRoomAccess(1L, 10L)).thenReturn(false);
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/app/chat/rooms/10/messages", 1L);

        Message<?> result = sut.preSend(message, channel);

        Principal user = StompHeaderAccessor.wrap(message).getUser();
        assertThat(result).isNull();
        verify(applicationEventPublisher).publishEvent(
            new WebSocketErrorEvent(user.getName(), AuthorizationErrorCode.RESOURCE_ACCESS_DENIED)
        );
    }

    @Test
    @DisplayName("채팅방 접근 권한이 없는 멤버의 SUBSCRIBE 프레임은 에러 메시지를 전송하고 차단된다")
    void subscribe_without_access_publishes_error_and_blocks() {
        when(checkChatRoomAccessUseCase.hasChatRoomAccess(1L, 10L)).thenReturn(false);
        Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, "/topic/chat/rooms/10/messages", 1L);

        Message<?> result = sut.preSend(message, channel);

        Principal user = StompHeaderAccessor.wrap(message).getUser();
        assertThat(result).isNull();
        verify(applicationEventPublisher).publishEvent(
            new WebSocketErrorEvent(user.getName(), AuthorizationErrorCode.RESOURCE_ACCESS_DENIED)
        );
    }

    @Test
    @DisplayName("/topic 하위 경로로 직접 SEND하는 프레임은 CommonException이 발생한다")
    void send_directly_to_broker_topic_throws() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/topic/chat/rooms/10/messages", 1L);

        assertThatThrownBy(() -> sut.preSend(message, channel))
            .isInstanceOf(CommonException.class)
            .extracting("baseCode")
            .isEqualTo(CommonErrorCode.SECURITY_WEBSOCKET_BROKER_ACCESS);
        verifyNoInteractions(checkChatRoomAccessUseCase);
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    @DisplayName("/topic 경로로 직접 SEND하는 프레임은 CommonException이 발생한다")
    void send_directly_to_exact_broker_topic_throws() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/topic", 1L);

        assertThatThrownBy(() -> sut.preSend(message, channel))
            .isInstanceOf(CommonException.class)
            .extracting("baseCode")
            .isEqualTo(CommonErrorCode.SECURITY_WEBSOCKET_BROKER_ACCESS);
        verifyNoInteractions(checkChatRoomAccessUseCase);
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    @DisplayName("/queue 하위 경로로 직접 SEND하는 프레임은 CommonException이 발생한다")
    void send_directly_to_broker_queue_throws() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/queue/errors", 1L);

        assertThatThrownBy(() -> sut.preSend(message, channel))
            .isInstanceOf(CommonException.class)
            .extracting("baseCode")
            .isEqualTo(CommonErrorCode.SECURITY_WEBSOCKET_BROKER_ACCESS);
        verifyNoInteractions(checkChatRoomAccessUseCase);
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    @DisplayName("/queue 경로로 직접 SEND하는 프레임은 CommonException이 발생한다")
    void send_directly_to_exact_broker_queue_throws() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/queue", 1L);

        assertThatThrownBy(() -> sut.preSend(message, channel))
            .isInstanceOf(CommonException.class)
            .extracting("baseCode")
            .isEqualTo(CommonErrorCode.SECURITY_WEBSOCKET_BROKER_ACCESS);
        verifyNoInteractions(checkChatRoomAccessUseCase);
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    @DisplayName("Principal이 없는 채팅방 프레임은 CommonException이 발생한다")
    void chat_frame_without_principal_throws() {
        Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, "/topic/chat/rooms/10/messages", null);

        assertThatThrownBy(() -> sut.preSend(message, channel))
            .isInstanceOf(CommonException.class)
            .extracting("baseCode")
            .isEqualTo(CommonErrorCode.SECURITY_NOT_GIVEN);
    }

    @Test
    @DisplayName("SEND와 SUBSCRIBE가 아닌 프레임은 인가 처리 없이 통과된다")
    void non_target_command_passes() {
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, "/topic/chat/rooms/10/messages", 1L);

        assertThat(sut.preSend(message, channel)).isSameAs(message);
        verifyNoInteractions(checkChatRoomAccessUseCase);
    }

    @Test
    @DisplayName("SUBSCRIBE의 chat namespace 안에서 잘못된 destination은 CommonException이 발생한다")
    void subscribe_invalid_chat_destination_throws() {
        List.of(
                "/topic/chat",
                "/topic/chat/",
                "/topic/chat/not-number",
                "/topic/chat/*",
                "/topic/chat/10/extra",
                "/topic/chat/10",
                "/topic/chat/rooms",
                "/topic/chat/rooms/",
                "/topic/chat/rooms/not-number/messages",
                "/topic/chat/rooms/*/messages",
                "/topic/chat/rooms/10",
                "/topic/chat/rooms/10/messages/extra"
            )
            .forEach(destination -> assertInvalidDestination(StompCommand.SUBSCRIBE, destination));

        verifyNoInteractions(checkChatRoomAccessUseCase);
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    @DisplayName("SEND의 chat namespace 안에서 잘못된 destination은 CommonException이 발생한다")
    void send_invalid_chat_destination_throws() {
        List.of(
                "/app/chat",
                "/app/chat/",
                "/app/chat/not-number",
                "/app/chat/*",
                "/app/chat/10/extra",
                "/app/chat/10",
                "/app/chat/rooms",
                "/app/chat/rooms/",
                "/app/chat/rooms/not-number/messages",
                "/app/chat/rooms/*/messages",
                "/app/chat/rooms/10",
                "/app/chat/rooms/10/messages/extra"
            )
            .forEach(destination -> assertInvalidDestination(StompCommand.SEND, destination));

        verifyNoInteractions(checkChatRoomAccessUseCase);
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    @DisplayName("chat namespace가 아닌 application destination은 인가 처리 없이 통과된다")
    void non_chat_application_destination_passes() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/app/inquiry/10", 1L);

        assertThat(sut.preSend(message, channel)).isSameAs(message);
        verifyNoInteractions(checkChatRoomAccessUseCase);
    }

    private void assertInvalidDestination(StompCommand command, String destination) {
        Message<byte[]> message = stompMessage(command, destination, 1L);

        assertThatThrownBy(() -> sut.preSend(message, channel))
            .isInstanceOf(CommonException.class)
            .extracting("baseCode")
            .isEqualTo(CommonErrorCode.SECURITY_WEBSOCKET_INVALID_DESTINATION);
    }

    private Message<byte[]> stompMessage(StompCommand command, String destination, Long memberId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        if (memberId != null) {
            MemberPrincipal principal = MemberPrincipal.builder().memberId(memberId).build();
            accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
