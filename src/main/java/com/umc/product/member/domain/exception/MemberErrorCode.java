package com.umc.product.member.domain.exception;

import org.springframework.http.HttpStatus;

import com.umc.product.global.response.code.BaseCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MemberErrorCode implements BaseCode {

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER-0001", "사용자를 찾을 수 없어요. 선택한 사용자를 확인해주세요."),
    MEMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "MEMBER-0002", "이미 등록된 사용자예요. 기존 계정을 확인해주세요."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "MEMBER-0003", "이미 사용 중인 이메일이에요. 다른 이메일을 입력해주세요."),
    MEMBER_ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST, "MEMBER-0004", "이미 탈퇴한 사용자예요. 다른 계정으로 진행해주세요."),
    INVALID_MEMBER_STATUS(HttpStatus.BAD_REQUEST, "MEMBER-0005", "사용자 상태가 올바르지 않아요. 상태를 확인해주세요."),
    MEMBER_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "MEMBER-0006", "활동 중인 사용자만 이용할 수 있어요. 계정 상태를 확인해주세요."),
    MEMBER_ALREADY_REGISTERED(HttpStatus.BAD_REQUEST, "MEMBER-0007", "이미 회원가입을 완료한 사용자예요. 로그인해주세요."),
    MEMBER_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER-0008", "프로필을 찾을 수 없어요. 프로필 정보를 확인해주세요."),
    MEMBER_SCHOOL_NOT_ASSIGNED(HttpStatus.BAD_REQUEST, "MEMBER-0009", "학교가 등록되지 않은 사용자예요. 학교 정보를 먼저 등록해주세요."),
    CREDENTIAL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "MEMBER-0010", "이미 로그인 ID와 비밀번호가 등록되어 있어요. 기존 정보로 로그인해주세요."),
    CREDENTIAL_NOT_REGISTERED(HttpStatus.BAD_REQUEST, "MEMBER-0011", "로그인 ID와 비밀번호가 등록되어 있지 않아요. 먼저 등록해주세요."),
    INVALID_LOGIN_ID(HttpStatus.BAD_REQUEST, "MEMBER-0012", "로그인 ID가 올바르지 않아요. 다시 입력해주세요."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "MEMBER-0013", "비밀번호가 올바르지 않아요. 다시 입력해주세요."),
    MEMBER_SEARCH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "MEMBER-0014",
        "회원을 검색할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
