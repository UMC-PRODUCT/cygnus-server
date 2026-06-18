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
    USER_FEEDBACK_TEMPLATE_ALREADY_ACTIVE(HttpStatus.CONFLICT, "FEEDBACK-0002", "이미 활성화된 피드백 양식이 있어요."),
    USER_FEEDBACK_TEMPLATE_INACTIVE(HttpStatus.CONFLICT, "FEEDBACK-0003", "비활성화된 피드백 양식이에요."),
    USER_FEEDBACK_TEMPLATE_TARGET_MISMATCH(HttpStatus.FORBIDDEN, "FEEDBACK-0004", "이 피드백 양식에 응답할 수 없는 대상이에요."),
    FEEDBACK_TEMPLATE_INVALID_FORM_STRUCTURE(HttpStatus.BAD_REQUEST, "FEEDBACK-0005", "피드백 양식 구조가 올바르지 않아요."),
    FEEDBACK_TEMPLATE_DESTRUCTIVE_CHANGE_NOT_ALLOWED(HttpStatus.CONFLICT, "FEEDBACK-0006", "이미 응답이 있는 피드백 양식은 질문, 섹션, 선택지를 삭제하거나 질문 타입을 변경할 수 없어요."),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
