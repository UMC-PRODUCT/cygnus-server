package com.umc.product.demoday.domain.exception;

import org.springframework.http.HttpStatus;

import com.umc.product.global.response.code.BaseCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DemodayErrorCode implements BaseCode {

    DEMODAY_VOTE_EVENT_INVALID_WINDOW(HttpStatus.BAD_REQUEST, "DEMODAY-0100", "투표 시작 시각은 종료 시각보다 앞서야 해요."),
    DEMODAY_VOTE_EVENT_BOOTH_LOCKED(HttpStatus.CONFLICT, "DEMODAY-0101", "투표가 시작되어 부스를 추가할 수 없어요."),

    DEMODAY_BOOTH_INVALID_IDENTIFIER(HttpStatus.BAD_REQUEST, "DEMODAY-0200", "부스는 등록된 프로젝트나 표시 이름 중 하나만 가져야 해요."),

    DEMODAY_ENTRY_CODE_ALREADY_REDEEMED(HttpStatus.CONFLICT, "DEMODAY-0300", "이미 사용된 인증 코드예요."),
    DEMODAY_ENTRY_CODE_ALREADY_BOUND(HttpStatus.CONFLICT, "DEMODAY-0301", "이미 다른 계정에 연결된 인증 코드예요."),

    DEMODAY_BALLOT_NOT_OPENED_YET(HttpStatus.CONFLICT, "DEMODAY-0400", "아직 투표 시간이 아니에요."),
    DEMODAY_BALLOT_CLOSED(HttpStatus.CONFLICT, "DEMODAY-0401", "투표가 종료되었어요."),
    DEMODAY_BALLOT_ALREADY_CAST(HttpStatus.CONFLICT, "DEMODAY-0402", "이미 투표를 완료했어요."),

    DEMODAY_STAMP_INVALID_OWNER(HttpStatus.BAD_REQUEST, "DEMODAY-0500", "스탬프 정보가 올바르지 않아요."),
    DEMODAY_STAMP_ALREADY_COLLECTED(HttpStatus.CONFLICT, "DEMODAY-0501", "이미 스탬프를 받은 부스예요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
