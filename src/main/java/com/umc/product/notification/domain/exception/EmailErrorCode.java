package com.umc.product.notification.domain.exception;

import org.springframework.http.HttpStatus;

import com.umc.product.global.response.code.BaseCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EmailErrorCode implements BaseCode {
    EMAIL_TEMPLATE_RENDER_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL-0004", "이메일 본문을 만들지 못했어요. 관리자에게 문의해주세요."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL-0005", "이메일을 보내지 못했어요. 잠시 후 다시 시도해주세요."),
    EMAIL_TEMPLATE_TYPE_INVALID(HttpStatus.BAD_REQUEST, "EMAIL-0006", "지원하지 않는 이메일 템플릿이에요."),
    EMAIL_RECIPIENT_INVALID(HttpStatus.BAD_REQUEST, "EMAIL-0007", "이메일 수신자 정보가 올바르지 않아요."),
    EMAIL_TEMPLATE_VARIABLES_INVALID(HttpStatus.BAD_REQUEST, "EMAIL-0008", "이메일 템플릿 변수 구성이 올바르지 않아요."),
    EMAIL_TEMPLATE_VARIABLE_INVALID(HttpStatus.BAD_REQUEST, "EMAIL-0009", "이메일 템플릿 변수 값이 올바르지 않아요."),
    EMAIL_TEMPLATE_ACTION_URL_INVALID(HttpStatus.BAD_REQUEST, "EMAIL-0010", "이메일 링크가 올바르지 않아요."),
    EMAIL_TEMPLATE_ACTION_ORIGIN_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "EMAIL-0011", "이메일 링크 도메인이 허용되지 않았어요."),
    EMAIL_TEMPLATE_REQUEST_INVALID(HttpStatus.BAD_REQUEST, "EMAIL-0012", "이메일 요청 정보가 올바르지 않아요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
