package com.umc.product.chat.application.port.out.dto;

/**
 * 방별 안 읽은 메시지 수 조회 결과.
 */
public record RoomUnreadCount(
    Long roomId,
    Long unreadCount
) {
}
