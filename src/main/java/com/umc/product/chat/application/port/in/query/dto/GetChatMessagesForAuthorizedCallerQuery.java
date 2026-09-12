package com.umc.product.chat.application.port.in.query.dto;

import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

/**
 * 호출자가 접근 권한을 사전에 검증했음을 전제로 하는 방 메시지 조회 입력.
 * <p>
 * {@link GetChatMessagesQuery}와 달리 memberId(요청자)를 받지 않는다 — membership 검사를 하지 않으므로
 * 애초에 필요가 없다. size 불변식은 동일하게 1..{@link GetChatMessagesQuery#MAX_PAGE_SIZE} 범위여야 한다.
 * 범위를 벗어나면 {@link ChatErrorCode#CHAT_MESSAGE_INVALID_PAGE_SIZE}.
 */
public record GetChatMessagesForAuthorizedCallerQuery(
    Long roomId,
    Long cursorId,
    int size
) {

    public GetChatMessagesForAuthorizedCallerQuery {
        if (size < 1 || size > GetChatMessagesQuery.MAX_PAGE_SIZE) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_INVALID_PAGE_SIZE);
        }
    }
}
