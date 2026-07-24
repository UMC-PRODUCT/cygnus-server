package com.umc.product.feedback.domain.exception;

import org.springframework.http.HttpStatus;

import com.umc.product.global.response.code.BaseCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FeedbackErrorCode implements BaseCode {

    // UserFeedback
    USER_FEEDBACK_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "FEEDBACK-0001", "피드백 양식을 찾을 수 없어요. 양식을 다시 선택해주세요."),
    USER_FEEDBACK_RESPONSE_FORBIDDEN(HttpStatus.FORBIDDEN, "FEEDBACK-0002", "해당 피드백 양식에 응답할 권한이 없어요."),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
