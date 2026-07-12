package com.umc.product.chat.application.port.out;

import com.umc.product.chat.domain.ChatRoom;

public interface LoadChatRoomPort {

    ChatRoom getById(Long roomId);

    /**
     * 방을 PESSIMISTIC_WRITE 락과 함께 조회한다(없으면 예외). 같은 방의 메시지 전송을 직렬화하기 위한 락 획득용.
     */
    ChatRoom getByIdForUpdate(Long roomId);
}
