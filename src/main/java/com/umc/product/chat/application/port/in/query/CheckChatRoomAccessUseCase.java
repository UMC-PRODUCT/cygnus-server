package com.umc.product.chat.application.port.in.query;

public interface CheckChatRoomAccessUseCase {

    boolean hasChatRoomAccess(Long memberId, Long chatRoomId);
}
