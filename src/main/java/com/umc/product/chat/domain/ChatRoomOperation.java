package com.umc.product.chat.domain;

/**
 * Chat engine이 소유하는 방 operation 목록이다.
 *
 * <p>소비 도메인별 권한 판단은 후속 policy dispatch에서 수행하고, Chat core는 이
 * operation vocabulary만 공개한다.</p>
 */
public enum ChatRoomOperation {
    CREATE,
    READ,
    SEND,
    JOIN,
    LEAVE,
    PIN,
    UNPIN,
    DELETE,
    MEMBERSHIP_MANAGE
}
