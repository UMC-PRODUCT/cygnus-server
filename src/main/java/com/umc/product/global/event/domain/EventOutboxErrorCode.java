package com.umc.product.global.event.domain;

import org.springframework.http.HttpStatus;

import com.umc.product.global.response.code.BaseCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EventOutboxErrorCode implements BaseCode {

    IDEMPOTENCY_CONFLICT(
        HttpStatus.CONFLICT,
        "EVENT-OUTBOX-0001",
        "동일한 식별자의 이벤트 요청이 기존 요청과 일치하지 않습니다."
    ),
    EVENT_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "EVENT-OUTBOX-0002",
        "요청한 이벤트 발행 정보를 찾을 수 없습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
