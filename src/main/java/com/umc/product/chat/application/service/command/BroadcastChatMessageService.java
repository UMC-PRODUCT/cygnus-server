package com.umc.product.chat.application.service.command;

import org.springframework.stereotype.Service;

import com.umc.product.chat.application.port.in.command.BroadcastChatMessageUseCase;
import com.umc.product.chat.application.port.in.command.dto.BroadcastChatMessagePayload;
import com.umc.product.chat.domain.event.ChatMessageCreatedEvent;
import com.umc.product.global.websocket.application.port.out.BroadcastPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 채팅 메시지 생성 이벤트를 수신하여 WebSocket broadcast 를 수행하는 커맨드 서비스.
 * <p>
 * destination 경로 조립은 이 서비스의 책임이며, {@link BroadcastPort} 는 전달받은 destination 으로 그대로 전송하기만 한다(브로커 교체 대비 디커플링).
 * <p>
 * <b>주의 — 구독 인가 전제:</b> 이 broadcast 는 공통 {@code /topic/chat/rooms/{roomId}/messages} 로 발행된다.
 * 이 토픽의 리소스 단위 SUBSCRIBE 인가(참여하지 않은 방의 수신 차단)를 소유할 소비 도메인이 아직 없으므로,
 * 공통 {@code StompAuthChannelInterceptor} 가 {@code /topic/chat/**} SUBSCRIBE 를 fail-closed 로 전면 차단한다.
 * 소비 도메인(inquiry 등)이 붙을 때 방별 authorizer(WS 구독 인가 공통 틀)로 이 전면 차단을 대체한다.
 * TODO: 그 전에 chat send 진입점을 먼저 열더라도, 구독 인가로 이 전면 차단을 대체하기 전에는 chat 토픽 구독이 열리지 않도록 유지할 것.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastChatMessageService implements BroadcastChatMessageUseCase {

    private static final String CHAT_ROOM_DESTINATION_PREFIX = "/topic/chat/rooms/";
    private static final String CHAT_ROOM_DESTINATION_SUFFIX = "/messages";

    private final BroadcastPort broadcastPort;

    @Override
    public void broadcast(ChatMessageCreatedEvent event) {
        String destination = CHAT_ROOM_DESTINATION_PREFIX + event.roomId() + CHAT_ROOM_DESTINATION_SUFFIX;

        BroadcastChatMessagePayload payload = BroadcastChatMessagePayload.from(event);

        log.debug("채팅 메시지 broadcast: messageId={}, destination={}", event.messageId(), destination);
        broadcastPort.broadcast(destination, payload);
    }
}
