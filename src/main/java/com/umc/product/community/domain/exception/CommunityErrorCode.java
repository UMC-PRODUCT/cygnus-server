package com.umc.product.community.domain.exception;

import org.springframework.http.HttpStatus;

import com.umc.product.global.response.code.BaseCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommunityErrorCode implements BaseCode {

    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY-0001", "게시글을 찾을 수 없어요. 목록을 새로고침해주세요."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY-0002", "댓글을 찾을 수 없어요. 목록을 새로고침해주세요."),

    INVALID_POST_TITLE(HttpStatus.BAD_REQUEST, "COMMUNITY-0004", "게시글 제목이 올바르지 않아요. 제목을 확인해주세요."),
    INVALID_POST_CONTENT(HttpStatus.BAD_REQUEST, "COMMUNITY-0005", "게시글 내용이 올바르지 않아요. 내용을 확인해주세요."),
    INVALID_POST_CATEGORY(HttpStatus.BAD_REQUEST, "COMMUNITY-0006", "게시글 카테고리가 올바르지 않아요. 카테고리를 다시 선택해주세요."),
    INVALID_POST_REGION(HttpStatus.BAD_REQUEST, "COMMUNITY-0007", "게시글 지역이 올바르지 않아요. 지역을 다시 선택해주세요."),
    CANNOT_CHANGE_TO_LIGHTNING(HttpStatus.BAD_REQUEST, "COMMUNITY-0008", "번개글은 번개글 작성 화면에서 만들어주세요."),
    CANNOT_CHANGE_FROM_LIGHTNING(HttpStatus.BAD_REQUEST, "COMMUNITY-0009", "번개글은 일반 게시글로 바꿀 수 없어요. 새 게시글로 작성해주세요."),

    INVALID_COMMENT_CONTENT(HttpStatus.BAD_REQUEST, "COMMUNITY-0010", "댓글 내용이 올바르지 않아요. 내용을 확인해주세요."),
    COMMENT_NOT_OWNED(HttpStatus.FORBIDDEN, "COMMUNITY-0011", "내가 작성한 댓글만 삭제할 수 있어요."),

    REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "COMMUNITY-0016", "이미 신고한 게시글, 댓글 또는 스레드 메시지예요. 신고 내역을 확인해주세요."),

    // === Post 검증 ===
    INVALID_POST_AUTHOR(HttpStatus.BAD_REQUEST, "COMMUNITY-0017", "작성자 정보가 필요해요. 로그인 정보를 확인해주세요."),
    NOT_LIGHTNING_POST(HttpStatus.BAD_REQUEST, "COMMUNITY-0018", "번개글이 아니에요. 일반 게시글 화면에서 수정해주세요."),
    USE_LIGHTNING_API(HttpStatus.BAD_REQUEST, "COMMUNITY-0019", "번개글은 번개글 화면에서 작성해주세요."),
    LIGHTNING_INFO_REQUIRED(HttpStatus.BAD_REQUEST, "COMMUNITY-0020", "번개글을 작성하려면 모임 정보를 입력해주세요."),
    POST_NOT_OWNED(HttpStatus.FORBIDDEN, "COMMUNITY-0021", "내가 작성한 게시글만 수정하거나 삭제할 수 있어요."),

    // === Lightning 검증 ===
    INVALID_LIGHTNING_MEET_AT(HttpStatus.BAD_REQUEST, "COMMUNITY-0022", "모임 시간을 입력해주세요."),
    INVALID_LIGHTNING_LOCATION(HttpStatus.BAD_REQUEST, "COMMUNITY-0023", "모임 장소를 입력해주세요."),
    INVALID_LIGHTNING_MAX_PARTICIPANTS(HttpStatus.BAD_REQUEST, "COMMUNITY-0024", "최대 참가자는 1명 이상으로 입력해주세요."),
    INVALID_LIGHTNING_OPEN_CHAT_URL(HttpStatus.BAD_REQUEST, "COMMUNITY-0025", "오픈 채팅 링크를 입력해주세요."),
    INVALID_LIGHTNING_OPEN_CHAT_URL_FORMAT(HttpStatus.BAD_REQUEST, "COMMUNITY-0026", "오픈 채팅 링크는 http:// 또는 https://로 시작해야 해요."),
    INVALID_LIGHTNING_MEET_AT_PAST(HttpStatus.BAD_REQUEST, "COMMUNITY-0027", "모임 시간은 현재 이후로 선택해주세요."),

    // === Comment 검증 ===
    INVALID_COMMENT_POST_ID(HttpStatus.BAD_REQUEST, "COMMUNITY-0028", "댓글을 작성할 게시글을 선택해주세요."),
    INVALID_COMMENT_CHALLENGER_ID(HttpStatus.BAD_REQUEST, "COMMUNITY-0029", "댓글 작성자 챌린저 정보를 확인해주세요."),

    // === 공통 ID 검증 ===
    INVALID_ID(HttpStatus.BAD_REQUEST, "COMMUNITY-0030", "ID는 1 이상의 숫자로 입력해주세요."),

    // === Adapter 검증 ===
    POST_SAVE_REQUIRES_AUTHOR(HttpStatus.BAD_REQUEST, "COMMUNITY-0031", "새 게시글을 만들려면 작성자 정보가 필요해요. 로그인 정보를 확인해주세요."),
    POST_UPDATE_INVALID_CALL(HttpStatus.BAD_REQUEST, "COMMUNITY-0032", "게시글 수정 요청이 올바르지 않아요. 요청 방식을 확인해주세요."),

    THREAD_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY-0033", "커뮤니티 스레드를 찾을 수 없어요."),
    THREAD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "COMMUNITY-0034", "커뮤니티 스레드에 접근할 권한이 없어요."),
    THREAD_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMUNITY-0035", "커뮤니티 스레드 멤버를 찾을 수 없어요."),
    THREAD_CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "COMMUNITY-0036", "커뮤니티 스레드의 최대 인원을 초과했어요."),
    THREAD_DELETED(HttpStatus.GONE, "COMMUNITY-0037", "삭제된 커뮤니티 스레드예요."),
    THREAD_MEMBER_ALREADY_ACTIVE(HttpStatus.CONFLICT, "COMMUNITY-0038", "이미 커뮤니티 스레드에 참여 중인 멤버예요."),
    THREAD_MEMBER_KICKED(HttpStatus.FORBIDDEN, "COMMUNITY-0039", "강퇴된 멤버는 커뮤니티 스레드에 다시 초대할 수 없어요."),
    THREAD_OWNER_REQUIRED(HttpStatus.FORBIDDEN, "COMMUNITY-0040", "커뮤니티 스레드 소유자만 수행할 수 있어요."),
    THREAD_OWNER_CANNOT_LEAVE(HttpStatus.CONFLICT, "COMMUNITY-0041", "소유권을 이전하기 전에는 커뮤니티 스레드를 나갈 수 없어요."),
    THREAD_OWNER_CANNOT_BE_KICKED(HttpStatus.CONFLICT, "COMMUNITY-0042", "커뮤니티 스레드 소유자는 강퇴할 수 없어요."),
    THREAD_INVALID_ROLE_CHANGE(HttpStatus.BAD_REQUEST, "COMMUNITY-0043", "커뮤니티 스레드 역할 변경 요청이 올바르지 않아요."),
    THREAD_INVITEE_NOT_ELIGIBLE(HttpStatus.BAD_REQUEST, "COMMUNITY-0044", "존재하는 활성 회원만 초대할 수 있어요."),
    THREAD_INVALID_COMMAND(HttpStatus.BAD_REQUEST, "COMMUNITY-0045", "커뮤니티 스레드 요청이 올바르지 않아요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
