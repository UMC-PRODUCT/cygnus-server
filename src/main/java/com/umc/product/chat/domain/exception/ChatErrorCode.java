package com.umc.product.chat.domain.exception;

import org.springframework.http.HttpStatus;

import com.umc.product.global.response.code.BaseCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChatErrorCode implements BaseCode {

    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT-0001", "채팅방을 찾을 수 없습니다."),
    CHAT_MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "CHAT-0002", "이미 채팅방에 참여 중인 멤버입니다."),
    CHAT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT-0003", "채팅방 멤버를 찾을 수 없습니다."),
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT-0004", "채팅 메시지를 찾을 수 없습니다."),
    CHAT_MESSAGE_INVALID_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "CHAT-0005", "허용되지 않는 메시지 콘텐츠 타입입니다."),
    CHAT_MESSAGE_EMPTY(HttpStatus.BAD_REQUEST, "CHAT-0006", "메시지 내용 또는 첨부가 필요합니다."),
    CHAT_ROOM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CHAT-0007", "해당 채팅방에 접근할 권한이 없습니다."),
    CHAT_MESSAGE_INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, "CHAT-0008", "허용되지 않는 페이지 크기입니다."),
    CHAT_MESSAGE_INVALID_REPLY_TARGET(HttpStatus.BAD_REQUEST, "CHAT-0009", "답장할 메시지를 찾을 수 없습니다."),
    CHAT_MESSAGE_ATTACHMENT_REQUIRED(HttpStatus.BAD_REQUEST, "CHAT-0010", "이미지 또는 파일 메시지에는 첨부파일이 필요합니다."),
    CHAT_MESSAGE_ATTACHMENT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "CHAT-0011", "텍스트 메시지에는 파일을 첨부할 수 없습니다."),
    CHAT_MESSAGE_INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "CHAT-0012", "메시지 타입에 허용되지 않는 파일 형식입니다."),
    CHAT_MESSAGE_INVALID_ATTACHMENT(HttpStatus.BAD_REQUEST, "CHAT-0013", "첨부파일 정보가 올바르지 않습니다."),
    CHAT_MESSAGE_IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, "CHAT-0014", "같은 메시지 식별자가 다른 내용에 사용되었습니다."),
    CHAT_MESSAGE_MUTATION_FORBIDDEN(HttpStatus.FORBIDDEN, "CHAT-0015", "해당 메시지를 변경할 권한이 없습니다."),
    CHAT_MESSAGE_INVALID_CONTENT_LENGTH(HttpStatus.BAD_REQUEST, "CHAT-0016", "메시지 내용 길이가 허용 범위를 벗어났습니다."),
    CHAT_MESSAGE_INVALID_MENTION(HttpStatus.BAD_REQUEST, "CHAT-0017", "멘션 대상이 채팅방 멤버가 아닙니다."),
    CHAT_MESSAGE_INVALID_REACTION(HttpStatus.BAD_REQUEST, "CHAT-0018", "리액션은 하나의 이모지여야 합니다."),
    CHAT_MESSAGE_REACTION_NOT_ALLOWED(HttpStatus.CONFLICT, "CHAT-0019", "삭제되었거나 시스템 메시지에는 리액션할 수 없습니다."),
    CHAT_MESSAGE_INVALID_ATTACHMENT_COUNT(HttpStatus.BAD_REQUEST, "CHAT-0020", "이미지는 1개 이상 4개 이하만 첨부할 수 있습니다."),
    CHAT_MESSAGE_ATTACHMENT_TOO_LARGE(HttpStatus.BAD_REQUEST, "CHAT-0021", "첨부파일 크기가 허용 범위를 초과했습니다."),
    CHAT_MESSAGE_CLIENT_ID_REQUIRED(HttpStatus.BAD_REQUEST, "CHAT-0022", "클라이언트 메시지 식별자가 필요합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
