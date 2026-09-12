package com.umc.product.chat.application.port.in.query;

import com.umc.product.chat.application.port.in.query.dto.ChatMessageCursorResult;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessagesForAuthorizedCallerQuery;

/**
 * 호출자가 접근 권한을 이미 검증했음을 전제로 방 메시지를 조회하는 유스케이스.
 * <p>
 * <b>계약: 이 유스케이스는 membership(참여자 여부) 검사를 하지 않는다.</b> 호출자(소비 도메인)가 자신의 책임으로
 * 접근 권한을 검증한 뒤에만 호출해야 한다. 일반적인 멤버 전용 조회 경로에는 절대 사용하지 말 것 — 그 경우엔
 * membership 검사를 수행하는 {@link GetChatMessagesUseCase}를 사용해야 한다.
 * <p>
 * chat은 이 유스케이스를 어떤 소비 도메인이 어떤 이유로 호출하는지 알지 못한다(엔진의 소비자 비인지 원칙). 이 조회로
 * 방 참여자(ChatMember)가 새로 등록되는 일은 없다 — 순수 조회다.
 */
public interface GetChatMessagesForAuthorizedCallerUseCase {

    ChatMessageCursorResult getMessages(GetChatMessagesForAuthorizedCallerQuery query);
}
