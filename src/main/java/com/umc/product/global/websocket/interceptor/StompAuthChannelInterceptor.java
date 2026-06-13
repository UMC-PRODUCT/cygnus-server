package com.umc.product.global.websocket.interceptor;

import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.chat.application.port.in.query.CheckChatRoomAccessUseCase;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.websocket.handler.WebSocketErrorEvent;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    // 클라이언트가 채팅 메시지를 서버로 보낼 때 사용하는 주소
    private static final String CHAT_APP_DESTINATION = "/app/chat";
    private static final String CHAT_APP_MESSAGE_PREFIX = CHAT_APP_DESTINATION + "/rooms/";
    // 클라이언트가 채팅방 메시지를 받기 위해 구독하는 주소
    private static final String CHAT_TOPIC_DESTINATION = "/topic/chat";
    private static final String CHAT_TOPIC_MESSAGE_PREFIX = CHAT_TOPIC_DESTINATION + "/rooms/";
    private static final String CHAT_MESSAGE_SUFFIX = "/messages";
    // 클라이언트가 직접 SEND할 수 없는 브로커 경로
    private static final String BROKER_TOPIC_PREFIX = "/topic";
    private static final String BROKER_QUEUE_PREFIX = "/queue";

    private final CheckChatRoomAccessUseCase checkChatRoomAccessUseCase;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 클라이언트에서 들어오는 STOMP 프레임 중 채팅방 접근 권한이 필요한 프레임을 검사한다.
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !isAuthorizationTarget(accessor.getCommand())) {
            return message;
        }

        if (StompCommand.SEND.equals(accessor.getCommand()) && isBrokerDestination(accessor.getDestination())) {
            throw new CommonException(CommonErrorCode.SECURITY_WEBSOCKET_BROKER_ACCESS);
        }

        ChatDestination chatDestination = parseChatDestination(accessor.getCommand(), accessor.getDestination());
        if (!chatDestination.target()) {
            return message;
        }
        if (!chatDestination.valid()) {
            throw new CommonException(CommonErrorCode.SECURITY_WEBSOCKET_INVALID_DESTINATION);
        }

        Principal user = accessor.getUser();
        Long memberId = extractMemberId(user);
        if (memberId == null) {
            throw new CommonException(CommonErrorCode.SECURITY_NOT_GIVEN);
        }
        if (!checkChatRoomAccessUseCase.hasChatRoomAccess(memberId, chatDestination.chatRoomId())) {
            log.warn("WebSocket 채팅방 접근 거부: memberId={}, chatRoomId={}, command={}, destination={}",
                memberId, chatDestination.chatRoomId(), accessor.getCommand(), accessor.getDestination());
            applicationEventPublisher.publishEvent(
                new WebSocketErrorEvent(user.getName(), AuthorizationErrorCode.RESOURCE_ACCESS_DENIED)
            );
            return null;
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

    /**
     * 채팅방 접근 권한 검사가 필요한 STOMP 명령인지 확인한다.
     */
    private boolean isAuthorizationTarget(StompCommand command) {
        return StompCommand.SUBSCRIBE.equals(command) || StompCommand.SEND.equals(command);
    }

    /**
     * STOMP 명령에 맞는 채팅 destination을 판별하고 채팅방 ID를 파싱한다.
     */
    private ChatDestination parseChatDestination(StompCommand command, String destination) {
        if (StompCommand.SUBSCRIBE.equals(command)) {
            return parseChatDestination(destination, CHAT_TOPIC_DESTINATION, CHAT_TOPIC_MESSAGE_PREFIX);
        }
        if (StompCommand.SEND.equals(command)) {
            return parseChatDestination(destination, CHAT_APP_DESTINATION, CHAT_APP_MESSAGE_PREFIX);
        }
        return ChatDestination.notTarget();
    }

    /**
     * /app/chat/rooms/{id}/messages, /topic/chat/rooms/{id}/messages 형식이면 id를 파싱하고,
     * chat 경로가 아니면 검사 대상에서 제외한다.
     */
    private ChatDestination parseChatDestination(String destination, String namespace, String prefix) {
        if (destination == null || !hasDestinationPrefix(destination, namespace)) {
            return ChatDestination.notTarget();
        }
        if (destination.equals(namespace)) {
            return ChatDestination.invalid();
        }

        if (!destination.startsWith(prefix) || !destination.endsWith(CHAT_MESSAGE_SUFFIX)) {
            return ChatDestination.invalid();
        }

        String rawChatRoomId = destination.substring(prefix.length(), destination.length() - CHAT_MESSAGE_SUFFIX.length());
        if (rawChatRoomId.isBlank() || rawChatRoomId.contains("/")) {
            return ChatDestination.invalid();
        }

        try {
            return ChatDestination.valid(Long.parseLong(rawChatRoomId));
        } catch (NumberFormatException ignored) {
            return ChatDestination.invalid();
        }
    }

    /**
     * STOMP 사용자 정보에서 인증된 회원 ID를 꺼낸다.
     */
    private Long extractMemberId(Principal user) {
        if (user instanceof UsernamePasswordAuthenticationToken auth
            && auth.getPrincipal() instanceof MemberPrincipal principal) {
            return principal.getMemberId();
        }
        return null;
    }

    /**
     * 채팅 destination 파싱 결과.
     *
     * @param target     chat namespace 여부
     * @param valid      채팅방 ID 형식 유효성
     * @param chatRoomId 추출된 채팅방 ID
     */
    private record ChatDestination(boolean target, boolean valid, Long chatRoomId) {

        /**
         * chat namespace가 아닌 destination.
         */
        private static ChatDestination notTarget() {
            return new ChatDestination(false, false, null);
        }

        /**
         * chat namespace 내부지만 형식이 잘못된 destination.
         */
        private static ChatDestination invalid() {
            return new ChatDestination(true, false, null);
        }

        /**
         * chat namespace 내부이며 채팅방 ID가 유효한 destination.
         */
        private static ChatDestination valid(Long chatRoomId) {
            return new ChatDestination(true, true, chatRoomId);
        }
    }
}
