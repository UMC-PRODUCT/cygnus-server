package com.umc.product.chat.domain.exception;

import com.umc.product.global.response.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChatErrorCode implements BaseCode {

    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT-0001", "채팅방을 찾을 수 없습니다."),
    CHAT_MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "CHAT-0002", "이미 채팅방에 참여 중인 멤버입니다."),
    CHAT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT-0003", "채팅방 멤버를 찾을 수 없습니다."),
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT-0004", "채팅 메시지를 찾을 수 없습니다."),
    CHAT_MESSAGE_INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "CHAT-0005", "허용되지 않는 메시지 콘텐츠 타입입니다."),
    CHAT_MESSAGE_EMPTY(HttpStatus.BAD_REQUEST, "CHAT-0006", "메시지 내용 또는 첨부가 필요합니다."),
    CHAT_ROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CHAT-0007", "해당 채팅방에 접근할 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
