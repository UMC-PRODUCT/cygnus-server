package com.umc.product.chat.application.port.in.command;

import com.umc.product.chat.domain.event.ChatMessageCreatedEvent;

/**
 * 채팅 메시지 생성 이벤트를 구독자에게 실시간 broadcast 하는 인바운드 포트.
 */
public interface BroadcastChatMessageUseCase {

    /**
     * 채팅 메시지 생성 이벤트를 수신하여 해당 채팅방 구독자들에게 broadcast 한다.
     *
     * @param event 생성된 채팅 메시지의 이벤트 데이터
     */
    void broadcast(ChatMessageCreatedEvent event);
}
