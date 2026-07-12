package com.umc.product.form.domain.exception;

import org.springframework.http.HttpStatus;

import com.umc.product.global.response.code.BaseCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FormErrorCode implements BaseCode {

    FORM_NOT_FOUND(HttpStatus.NOT_FOUND, "FORM-0001", "폼을 찾을 수 없어요. 선택한 폼을 확인해주세요."),
    FORM_NOT_DRAFT(HttpStatus.CONFLICT, "FORM-0002", "임시저장 상태의 폼만 편집할 수 있어요. 폼 상태를 확인해주세요."),
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "FORM-0003", "질문을 찾을 수 없어요. 선택한 질문을 확인해주세요."),
    FORM_ALREADY_PUBLISHED(HttpStatus.BAD_REQUEST, "FORM-0005", "이미 발행된 폼이에요. 폼 상태를 확인해주세요."),
    FORM_RESPONSE_NOT_FOUND(HttpStatus.NOT_FOUND, "FORM-0006", "폼 응답을 찾을 수 없어요. 응답 목록을 확인해주세요."),
    QUESTION_IS_NOT_OWNED_BY_FORM(HttpStatus.BAD_REQUEST, "FORM-0007", "이 폼에 포함된 질문이 아니에요. 질문을 다시 선택해주세요."),
    FORM_RESPONSE_FORBIDDEN(HttpStatus.FORBIDDEN, "FORM-0008",
        "이 폼 응답에 접근할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."),
    QUESTION_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "FORM-0009", "질문 유형이 맞지 않아요. 질문 유형을 확인해주세요."),
    REQUIRED_QUESTION_NOT_ANSWERED(HttpStatus.BAD_REQUEST, "FORM-0010", "필수 질문에 답변해주세요."),
    INVALID_ANSWER_FORMAT(HttpStatus.BAD_REQUEST, "FORM-0011", "응답 형식이 올바르지 않아요. 답변을 확인해주세요."),
    OTHER_OPTION_DUPLICATED(HttpStatus.BAD_REQUEST, "FORM-0012", "'기타' 선택지가 중복됐어요. 선택지를 확인해주세요."),
    OPTION_NOT_IN_QUESTION(HttpStatus.BAD_REQUEST, "FORM-0013", "해당 질문에 없는 선택지예요. 선택지를 다시 선택해주세요."),
    OPTION_TEXT_REQUIRED(HttpStatus.BAD_REQUEST, "FORM-0014", "'기타' 선택지의 내용을 입력해주세요."),
    INVALID_FORM_ACTIVE_PERIOD(HttpStatus.BAD_REQUEST, "FORM-0015", "폼 응답 가능 기간이 올바르지 않아요. 기간을 다시 선택해주세요."),
    // FORM-0016 ~ FORM-0022 (투표 항목/기간/상태 관련): notice 도메인으로 이관
    INVALID_VOTE_SELECTION(HttpStatus.BAD_REQUEST, "FORM-0023", "투표 선택이 올바르지 않아요. 선택지를 확인해주세요."),
    // FORM-0024 (INVALID_VOTE_QUESTION_TYPE): notice 도메인으로 이관
    INVALID_VOTE_FORM_STRUCTURE(HttpStatus.BAD_REQUEST, "FORM-0025", "투표 질문 형식이 올바르지 않아요. 투표 구성을 확인해주세요."),
    // FORM-0026 (VOTE_RESPONSE_NOT_FOUND): notice 도메인으로 이관
    FORM_RESPONSE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "FORM-0027", "이미 제출한 응답이 있어요. 제출 내역을 확인해주세요."),
    FORM_NOT_PUBLISHED(HttpStatus.CONFLICT, "FORM-0028", "발행된 폼에만 응답할 수 있어요. 폼 상태를 확인해주세요."),
    QUESTION_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "FORM-0029", "선택지를 찾을 수 없어요. 선택지를 다시 확인해주세요."),
    ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "FORM-0030", "답변을 찾을 수 없어요. 응답 내용을 확인해주세요."),
    FORM_RESPONSE_NOT_DRAFT(HttpStatus.CONFLICT, "FORM-0031", "임시저장 상태의 응답에서만 할 수 있는 작업이에요. 응답 상태를 확인해주세요."),
    ANSWER_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "FORM-0032", "이미 해당 질문에 대한 답변이 있어요. 기존 답변을 수정해주세요."),
    FORM_RESPONSE_LOOKUP_AMBIGUOUS(HttpStatus.CONFLICT, "FORM-0033",
        "중복 응답을 허용하는 폼은 응답을 하나로 특정할 수 없어요. 응답 ID를 사용해주세요."),
    RESPONDENT_MEMBER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "FORM-0034",
        "응답자 정보가 필요해요. 이 문제가 계속되면 운영진에게 문의해주세요."),
    RESPONSE_ACCESS_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "FORM-0035",
        "응답 접근 키가 필요해요. 이 문제가 계속되면 운영진에게 문의해주세요."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
