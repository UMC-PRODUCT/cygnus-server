package com.umc.product.challenger.domain.exception;

import org.springframework.http.HttpStatus;

import com.umc.product.global.response.code.BaseCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChallengerErrorCode implements BaseCode {

    CHALLENGER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHALLENGER-0001", "챌린저를 찾을 수 없어요. 선택한 챌린저를 확인해주세요."),
    CHALLENGER_ALREADY_EXISTS(HttpStatus.CONFLICT, "CHALLENGER-0002", "이미 등록된 챌린저예요. 기존 기록을 확인해주세요."),
    CHALLENGER_ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST, "CHALLENGER-0003", "이미 탈퇴한 챌린저예요. 다른 챌린저를 선택해주세요."),
    INVALID_CHALLENGER_STATUS(HttpStatus.BAD_REQUEST, "CHALLENGER-0004", "챌린저 상태가 올바르지 않아요. 상태를 확인해주세요."),
    CHALLENGER_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "CHALLENGER-0005", "활동 중인 챌린저만 사용할 수 있어요. 챌린저 상태를 확인해주세요."),
    CHALLENGER_POINT_NOT_FOUND(HttpStatus.NOT_FOUND, "CHALLENGER-0007", "상벌점 기록을 찾을 수 없어요. 선택한 기록을 확인해주세요."),
    BAD_CHALLENGER_UPDATE_REQUEST(HttpStatus.NOT_FOUND, "CHALLENGER-0008", "챌린저 수정 요청이 올바르지 않아요. 입력값을 확인해주세요."),
    NOT_ALLOWED_AUTHOR(HttpStatus.BAD_REQUEST, "CHALLENGER-0009", "일정을 만들려면 챌린저 상태가 활동 중이거나 수료여야 해요."),
    MEMBER_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "CHALLENGER-0010", "연결된 멤버 프로필을 찾을 수 없어요. 회원 정보를 확인해주세요."),
    INVALID_CURSOR_ID(HttpStatus.BAD_REQUEST, "CHALLENGER-0011", "커서 값이 올바르지 않아요. 목록을 처음부터 다시 조회해주세요."),
    USED_CHALLENGER_RECORD_CODE(HttpStatus.BAD_REQUEST, "CHALLENGER-0012", "이미 사용한 챌린저 기록 추가 코드예요. 새 코드를 발급받아주세요."),
    INVALID_MEMBER_NAME_FOR_RECORD(HttpStatus.BAD_REQUEST, "CHALLENGER-0013", "코드에 등록된 이름이 내 정보와 일치하지 않아요. 입력한 코드를 확인해주세요."),
    INVALID_SCHOOL_FOR_RECORD(HttpStatus.BAD_REQUEST, "CHALLENGER-0014", "코드에 등록된 학교가 내 소속과 일치하지 않아요. 소속 정보를 확인해주세요."),
    INVALID_CHALLENGER_RECORD_CREATE_REQUEST(HttpStatus.BAD_REQUEST, "CHALLENGER-0015", "입력한 정보로 챌린저 기록을 만들 수 없어요. 값을 확인해주세요."),
    NO_CHALLENGER_IN_MEMBER_GISU(HttpStatus.NOT_FOUND, "CHALLENGER-0016", "해당 기수의 챌린저 기록을 찾을 수 없어요. 기수를 확인해주세요."),
    CHALLENGER_PART_NOT_FOUND(HttpStatus.NOT_FOUND, "CHALLENGER-0017", "챌린저 파트를 찾을 수 없어요. 파트 값을 확인해주세요."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
