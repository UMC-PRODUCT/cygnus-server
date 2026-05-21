package com.umc.product.inquiry.domain.exception;

import com.umc.product.global.response.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum InquiryErrorCode implements BaseCode {

    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "INQUIRY-0001", "문의사항을 찾을 수 없습니다."),
    INQUIRY_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "INQUIRY-0002", "문의사항 메시지를 찾을 수 없습니다."),
    INQUIRY_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "INQUIRY-0003", "이미 종료된 문의사항입니다."),
    NO_INQUIRY_PERMISSION(HttpStatus.FORBIDDEN, "INQUIRY-0004", "해당 문의사항에 접근할 권한이 없습니다."),
    INQUIRY_INVALID_STATUS_FOR_PROGRESS(HttpStatus.BAD_REQUEST, "INQUIRY-0005",
        "접수(RECEIVED) 상태의 문의사항만 처리를 시작할 수 있습니다."),
    INQUIRY_INVALID_STATUS_FOR_REOPEN(HttpStatus.BAD_REQUEST, "INQUIRY-0006", "종료(CLOSED) 상태의 문의사항만 재오픈할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
