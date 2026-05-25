package com.umc.product.term.domain.exception;

import org.springframework.http.HttpStatus;

import com.umc.product.global.response.code.BaseCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TermErrorCode implements BaseCode {

    TERMS_NOT_FOUND(HttpStatus.NOT_FOUND, "TERMS-0001", "약관을 찾을 수 없어요. 선택한 약관을 확인해주세요."),
    TERMS_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "TERMS-0002", "약관 타입을 선택해주세요."),
    TERMS_TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "TERMS-0003", "약관 제목을 입력해주세요."),
    TERMS_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "TERMS-0004", "약관 내용을 입력해주세요."),
    TERMS_VERSION_REQUIRED(HttpStatus.BAD_REQUEST, "TERMS-0005", "약관 버전을 입력해주세요."),

    TERMS_CONSENT_NOT_FOUND(HttpStatus.NOT_FOUND, "TERMS-0006", "약관 동의 정보를 찾을 수 없어요. 동의 내역을 확인해주세요."),
    TERMS_CONSENT_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "TERMS-0007", "이미 동의한 약관이에요. 동의 내역을 확인해주세요."),
    MEMBER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "TERMS-0008", "회원을 선택해주세요."),
    TERM_ID_REQUIRED(HttpStatus.BAD_REQUEST, "TERMS-0009", "약관을 선택해주세요."),
    MANDATORY_TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "TERMS-0010", "필수 약관에 모두 동의해주세요."),

    TERM_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "TERMS-0011",
        "약관을 관리할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."),
    TERMS_RECONSENT_REQUIRED(HttpStatus.FORBIDDEN, "TERMS-0012", "변경된 필수 약관에 동의해주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
