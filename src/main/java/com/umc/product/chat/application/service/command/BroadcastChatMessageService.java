package com.umc.product.chat.application.service.command;

import com.umc.product.chat.application.port.in.command.BroadcastChatMessageUseCase;
import com.umc.product.chat.application.port.in.command.dto.BroadcastChatMessagePayload;
import com.umc.product.chat.domain.event.ChatMessageCreatedEvent;
import com.umc.product.global.websocket.application.port.out.BroadcastPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 채팅 메시지 생성 이벤트를 수신하여 WebSocket broadcast 를 수행하는 커맨드 서비스.
 * <p>
 * destination 경로 조립은 이 서비스의 책임이며, {@link BroadcastPort} 는 전달받은 destination 으로
 * 그대로 전송하기만 한다(브로커 교체 대비 디커플링).
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
