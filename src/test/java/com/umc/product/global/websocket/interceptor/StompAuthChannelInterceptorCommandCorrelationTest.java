package com.umc.product.global.websocket.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
import com.umc.product.global.websocket.handler.WebSocketErrorEvent;
import com.umc.product.global.websocket.handler.WebSocketErrorPayload;
import com.umc.product.global.websocket.handler.WebSocketErrorPublisher;
import com.umc.product.global.websocket.support.StompCommandIdParser;

@DisplayName("STOMP Community command correlation")
class StompAuthChannelInterceptorCommandCorrelationTest {

    private static final UUID COMMAND_ID = UUID.fromString("b108f0c7-e244-4c9d-a57a-6e5bb8e94e89");
    private static final UUID CLIENT_MESSAGE_ID =
        UUID.fromString("0dce06f4-11bc-4dc2-b9fd-4f9cb88ea9cd");

    @Test
    @DisplayName("정확히 한 소비 도메인 authorizer가 승인한 Community SEND는 통과된다")
    void authorizedCommunitySendPasses() {
        String destination = "/app/community/threads/10/messages";
        StompSendAuthorizerRegistry sendAuthorizerRegistry = mock(StompSendAuthorizerRegistry.class);
        given(sendAuthorizerRegistry.isAuthorized(10L, destination)).willReturn(true);
        StompAuthChannelInterceptor sut = interceptor(sendAuthorizerRegistry, new RecordingEventPublisher());
        Message<byte[]> message = communitySend(COMMAND_ID.toString());

        assertThat(sut.preSend(message, null)).isSameAs(message);
    }

    @Test
    @DisplayName("Community SEND 인가가 실패하면 correlated typed 오류 후 해당 프레임만 거부한다")
    void unauthorizedCommunitySendPublishesRecoverableError() {
        StompSendAuthorizerRegistry sendAuthorizerRegistry = mock(StompSendAuthorizerRegistry.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        StompAuthChannelInterceptor sut = interceptor(sendAuthorizerRegistry, eventPublisher);

        Message<?> result = sut.preSend(communitySend(COMMAND_ID.toString()), null);

        assertThat(result).isNull();
        ArgumentCaptor<WebSocketErrorEvent> eventCaptor = ArgumentCaptor.forClass(WebSocketErrorEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        WebSocketErrorEvent event = eventCaptor.getValue();
        assertThat(event.userName()).isEqualTo("10");
        assertThat(event.commandId()).isEqualTo(COMMAND_ID);
        assertThat(event.status()).isEqualTo(403);
        assertThat(event.code()).isEqualTo(CommonErrorCode.SECURITY_WEBSOCKET_INVALID_DESTINATION.getCode());
        assertThat(event.retryable()).isFalse();
    }

    @Test
    @DisplayName(
        "인가 저장소 장애는 correlated typed 500으로 해당 프레임만 버리고 같은 세션의 다음 SEND는 회복한다"
    )
    void authorizationInfrastructureFailureDropsOnlyCurrentFrameAndSessionRecovers() {
        String destination = "/app/community/threads/10/messages";
        StompSendAuthorizerRegistry sendAuthorizerRegistry = mock(StompSendAuthorizerRegistry.class);
        given(sendAuthorizerRegistry.isAuthorized(10L, destination))
            .willThrow(new IllegalStateException("database unavailable"))
            .willReturn(true);
        StompClientMessageIdResolverRegistry clientMessageIdResolverRegistry =
            mock(StompClientMessageIdResolverRegistry.class);
        given(clientMessageIdResolverRegistry.resolve(any(), any()))
            .willReturn(Optional.of(CLIENT_MESSAGE_ID));
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        WebSocketErrorPublisher errorPublisher = new WebSocketErrorPublisher(messagingTemplate);
        List<WebSocketErrorEvent> events = new ArrayList<>();
        ApplicationEventPublisher eventPublisher = event -> {
            WebSocketErrorEvent errorEvent = (WebSocketErrorEvent) event;
            events.add(errorEvent);
            errorPublisher.sendErrorToUser(errorEvent);
        };
        StompAuthChannelInterceptor sut = interceptor(
            sendAuthorizerRegistry,
            clientMessageIdResolverRegistry,
            eventPublisher
        );
        Message<byte[]> failedFrame = communitySend(COMMAND_ID.toString());
        Message<byte[]> recoveredFrame = communitySend(COMMAND_ID.toString());

        Message<?> failedResult = sut.preSend(failedFrame, null);
        Message<?> recoveredResult = sut.preSend(recoveredFrame, null);

        assertThat(failedResult).isNull();
        assertThat(recoveredResult).isSameAs(recoveredFrame);
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.userName()).isEqualTo("10");
            assertThat(event.commandId()).isEqualTo(COMMAND_ID);
            assertThat(event.clientMessageId()).isEqualTo(CLIENT_MESSAGE_ID);
            assertThat(event.status()).isEqualTo(500);
            assertThat(event.code()).isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode());
            assertThat(event.message()).isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage());
            assertThat(event.retryable()).isTrue();
        });
        verify(messagingTemplate).convertAndSendToUser(
            eq("10"),
            eq("/queue/errors"),
            any(WebSocketErrorPayload.class)
        );
    }

    @Test
    @DisplayName("x-command-id가 누락되거나 canonical UUID가 아니면 non-terminal typed 400으로 거부한다")
    void missingOrMalformedCommandIdPublishesTypedValidationError() {
        StompSendAuthorizerRegistry sendAuthorizerRegistry = mock(StompSendAuthorizerRegistry.class);
        RecordingEventPublisher eventPublisher = new RecordingEventPublisher();
        StompAuthChannelInterceptor sut = interceptor(sendAuthorizerRegistry, eventPublisher);

        Message<?> missingResult = sut.preSend(communitySend(null), null);
        Message<?> malformedResult = sut.preSend(communitySend("NOT-A-CANONICAL-UUID"), null);

        assertThat(missingResult).isNull();
        assertThat(malformedResult).isNull();
        assertThat(eventPublisher.events).hasSize(2).allSatisfy(event -> {
            assertThat(event.commandId()).isNull();
            assertThat(event.clientMessageId()).isNull();
            assertThat(event.status()).isEqualTo(400);
            assertThat(event.code()).isEqualTo(CommonErrorCode.BAD_REQUEST.getCode());
            assertThat(event.retryable()).isFalse();
        });
        verifyNoInteractions(sendAuthorizerRegistry);
    }

    @Test
    @DisplayName("인증 principal이 없는 Community SEND는 보안 오류로 즉시 거절한다")
    void communitySendWithoutPrincipalRejected() {
        StompSendAuthorizerRegistry sendAuthorizerRegistry = mock(StompSendAuthorizerRegistry.class);
        StompAuthChannelInterceptor sut = interceptor(
            sendAuthorizerRegistry,
            new RecordingEventPublisher()
        );
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/community/threads/10/messages");
        accessor.setNativeHeader(StompCommandIdParser.COMMAND_ID_HEADER, COMMAND_ID.toString());
        Message<byte[]> message = MessageBuilder.createMessage(
            new byte[0],
            accessor.getMessageHeaders()
        );

        assertThatThrownBy(() -> sut.preSend(message, null))
            .isInstanceOf(CommonException.class)
            .extracting(error -> ((CommonException) error).getBaseCode())
            .isEqualTo(CommonErrorCode.SECURITY_NOT_GIVEN);
        verifyNoInteractions(sendAuthorizerRegistry);
    }

    private StompAuthChannelInterceptor interceptor(
        StompSendAuthorizerRegistry sendAuthorizerRegistry,
        ApplicationEventPublisher eventPublisher
    ) {
        return new StompAuthChannelInterceptor(
            mock(StompSubscriptionAuthorizerRegistry.class),
            sendAuthorizerRegistry,
            new StompClientMessageIdResolverRegistry(List.of()),
            eventPublisher
        );
    }

    private StompAuthChannelInterceptor interceptor(
        StompSendAuthorizerRegistry sendAuthorizerRegistry,
        StompClientMessageIdResolverRegistry clientMessageIdResolverRegistry,
        ApplicationEventPublisher eventPublisher
    ) {
        return new StompAuthChannelInterceptor(
            mock(StompSubscriptionAuthorizerRegistry.class),
            sendAuthorizerRegistry,
            clientMessageIdResolverRegistry,
            eventPublisher
        );
    }

    private Message<byte[]> communitySend(String commandId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/community/threads/10/messages");
        if (commandId != null) {
            accessor.setNativeHeader(StompCommandIdParser.COMMAND_ID_HEADER, commandId);
        }
        accessor.setUser(new UsernamePasswordAuthenticationToken(
            new MemberPrincipal(10L),
            null,
            List.of()
        ));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static class RecordingEventPublisher implements ApplicationEventPublisher {

        private final List<WebSocketErrorEvent> events = new ArrayList<>();

        @Override
        public void publishEvent(Object event) {
            events.add((WebSocketErrorEvent) event);
        }
    }
}
