package com.umc.product.chat.application.port.in.query.dto;

/**
 * 채팅방 목록 항목. 마지막 메시지 미리보기와 안 읽은 수를 포함한다.
 *
 * @param lastMessage 아직 메시지가 없는 방이면 {@code null}
 */
public record ChatRoomSummaryInfo(
    Long roomId,
    ChatMessageInfo lastMessage,
    long unreadCount
) {
}
