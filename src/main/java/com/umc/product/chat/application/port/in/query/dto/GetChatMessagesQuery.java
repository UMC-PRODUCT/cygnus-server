package com.umc.product.chat.application.port.in.query.dto;

import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

/**
 * 방 메시지 커서 페이지네이션 조회 입력.
 * <p>
 * 외부 controller 를 제거한 뒤 이 record 가 엔진의 유일한 입력 경계이므로, size 불변식을 여기서 강제한다. size 는 1..{@link #MAX_PAGE_SIZE} 범위여야 한다. 범위를
 * 벗어나면 {@link ChatErrorCode#CHAT_MESSAGE_INVALID_PAGE_SIZE}.
 */
public record GetChatMessagesQuery(
    Long roomId,
    Long memberId,
    Long cursorId,
    int size
) {

    public static final int MAX_PAGE_SIZE = 50;

    public GetChatMessagesQuery {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_INVALID_PAGE_SIZE);
        }
    }
}
