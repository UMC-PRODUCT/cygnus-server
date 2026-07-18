package com.umc.product.global.websocket.interceptor;

import java.security.Principal;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.response.code.BaseCode;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.websocket.application.service.StompClientMessageIdResolverRegistry;
import com.umc.product.global.websocket.application.service.StompSendAuthorizerRegistry;
import com.umc.product.global.websocket.application.service.StompSubscriptionAuthorizerRegistry;
import com.umc.product.global.websocket.handler.WebSocketErrorEvent;
import com.umc.product.global.websocket.support.StompCommandIdParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 브로커 경로 보호를 위한 공통 STOMP 인터셉터.
 *
 * <p><b>현재 책임(공통):</b> 클라이언트가 broker destination({@code /topic}, {@code /queue})이나
 * 사용자 destination({@code /user})으로 <b>직접 발행</b> 하는 것을 차단한다.
 * 클라이언트가 server-only {@code MESSAGE} 명령을 조작하는 경우도 동일한 message type으로 차단한다.
 * broadcast 는 서버만 수행하며 클라이언트의 broker 직접 발행은 허용하지 않는다.
 *
 * <p><b>SUBSCRIBE 인가:</b><br>
 * broker destination 구독은 공통 registry를 통해 해당 경로를 소유한 소비 도메인 authorizer에 위임한다.
 * 지원하는 authorizer가 없거나 둘 이상이거나 인가에 실패하면 fail-closed 처리한다.
 *
 * @see <a href="file:../../../../../../../../../docs/adr/011-inquiry-domain-with-websocket-stomp.md">ADR-011</a>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    // 클라이언트가 직접 SEND할 수 없는 브로커 경로
    private static final String BROKER_TOPIC_PREFIX = "/topic";
    private static final String BROKER_QUEUE_PREFIX = "/queue";
    private static final String USER_DESTINATION_PREFIX = "/user";
    private static final String USER_ERROR_DESTINATION = "/user/queue/errors";
    private static final String COMMUNITY_APPLICATION_PREFIX = "/app/community";

    private final StompSubscriptionAuthorizerRegistry subscriptionAuthorizerRegistry;
    private final StompSendAuthorizerRegistry sendAuthorizerRegistry;
    private final StompClientMessageIdResolverRegistry clientMessageIdResolverRegistry;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        String destination = accessor.getDestination();

        if (StompCommand.MESSAGE.equals(command)) {
            throw new CommonException(CommonErrorCode.SECURITY_WEBSOCKET_INVALID_DESTINATION);
        }

        if (SimpMessageType.MESSAGE.equals(accessor.getMessageType())
            && (isBrokerDestination(destination) || isUserDestination(destination))) {
            throw new CommonException(CommonErrorCode.SECURITY_WEBSOCKET_BROKER_ACCESS);
        }

        if (StompCommand.SEND.equals(command) && isCommunityApplicationDestination(destination)) {
            if (!isCommunitySendAuthorized(message, accessor, destination)) {
                return null;
            }
        }

        if (StompCommand.SUBSCRIBE.equals(command)
            && isUserDestination(destination)
            && !USER_ERROR_DESTINATION.equals(destination)) {
            throw new CommonException(CommonErrorCode.SECURITY_WEBSOCKET_INVALID_DESTINATION);
        }

        if (StompCommand.SUBSCRIBE.equals(command) && isBrokerDestination(destination)) {
            Long memberId = extractMemberId(accessor.getUser());
            if (!subscriptionAuthorizerRegistry.isAuthorized(memberId, destination)) {
                throw new CommonException(CommonErrorCode.SECURITY_WEBSOCKET_INVALID_DESTINATION);
            }
        }

        return message;
    }

    private boolean isBrokerDestination(String destination) {
        return destination != null
            && (hasDestinationPrefix(destination, BROKER_TOPIC_PREFIX)
            || hasDestinationPrefix(destination, BROKER_QUEUE_PREFIX));
    }

    private Long extractMemberId(Principal principal) {
        if (principal instanceof Authentication authentication
            && authentication.getPrincipal() instanceof MemberPrincipal memberPrincipal) {
            return memberPrincipal.getMemberId();
        }
        return null;
    }

    private boolean isUserDestination(String destination) {
        return destination != null
            && (destination.equals(USER_DESTINATION_PREFIX)
            || destination.startsWith(USER_DESTINATION_PREFIX + "/"));
    }

    private boolean isCommunityApplicationDestination(String destination) {
        return destination != null
            && (destination.equals(COMMUNITY_APPLICATION_PREFIX)
            || destination.startsWith(COMMUNITY_APPLICATION_PREFIX + "/"));
    }

    private boolean isCommunitySendAuthorized(
        Message<?> message,
        StompHeaderAccessor accessor,
        String destination
    ) {
        Principal principal = accessor.getUser();
        if (principal == null) {
            throw new CommonException(CommonErrorCode.SECURITY_NOT_GIVEN);
        }

        UUID commandId = StompCommandIdParser.parse(accessor);
        if (commandId == null) {
            publishRecoverableSendError(
                principal,
                null,
                resolveClientMessageId(message, destination),
                CommonErrorCode.BAD_REQUEST,
                false
            );
            return false;
        }

        Long memberId = extractMemberId(principal);
        boolean authorized;
        try {
            authorized = sendAuthorizerRegistry.isAuthorized(memberId, destination);
        } catch (RuntimeException exception) {
            log.error(
                "STOMP SEND authorization failed: memberId={}, destination={}",
                memberId,
                destination,
                exception
            );
            publishRecoverableSendError(
                principal,
                commandId,
                resolveClientMessageId(message, destination),
                CommonErrorCode.INTERNAL_SERVER_ERROR,
                true
            );
            return false;
        }
        if (!authorized) {
            publishRecoverableSendError(
                principal,
                commandId,
                resolveClientMessageId(message, destination),
                CommonErrorCode.SECURITY_WEBSOCKET_INVALID_DESTINATION,
                false
            );
            return false;
        }
        return true;
    }

    private UUID resolveClientMessageId(Message<?> message, String destination) {
        return clientMessageIdResolverRegistry.resolve(destination, message.getPayload()).orElse(null);
    }

    private void publishRecoverableSendError(
        Principal principal,
        UUID commandId,
        UUID clientMessageId,
        BaseCode errorCode,
        boolean retryable
    ) {
        eventPublisher.publishEvent(WebSocketErrorEvent.from(
            principal.getName(),
            commandId,
            clientMessageId,
            errorCode,
            retryable
        ));
    }

    private boolean hasDestinationPrefix(String destination, String prefix) {
        return destination.startsWith(prefix);
    }
}
