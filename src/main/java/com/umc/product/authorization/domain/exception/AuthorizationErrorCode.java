package com.umc.product.authorization.domain.exception;

import org.springframework.http.HttpStatus;

import com.umc.product.global.response.code.BaseCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthorizationErrorCode implements BaseCode {

    PERMISSION_DENIED(HttpStatus.FORBIDDEN, "AUTHORIZATION-0001",
        "권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."),
    RESOURCE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTHORIZATION-0002",
        "이 항목에 접근할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."),
    INVALID_PERMISSION(HttpStatus.BAD_REQUEST, "AUTHORIZATION-0003", "권한 값이 올바르지 않아요. 요청 값을 확인해주세요."),
    POLICY_EVALUATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTHORIZATION-0004",
        "권한을 확인하지 못했어요. 잠시 후 다시 시도해주세요."),
    NO_EVALUATOR_MATCHING_RESOURCE_TYPE(HttpStatus.INTERNAL_SERVER_ERROR, "AUTHORIZATION-0005",
        "권한 확인 설정을 찾지 못했어요. 관리자에게 문의해주세요."),
    PERMISSION_TYPE_NOT_SUPPORTED_BY_RESOURCE_TYPE(HttpStatus.INTERNAL_SERVER_ERROR, "AUTHORIZATION-0006",
        "지원하지 않는 권한 유형이에요. 관리자에게 문의해주세요."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "AUTHORIZATION-0007", "권한 확인 요청이 올바르지 않아요. 요청 값을 확인해주세요."),
    INVALID_RESOURCE_ID_TYPE(HttpStatus.BAD_REQUEST, "AUTHORIZATION-0008",
        "권한을 확인할 항목 ID가 올바르지 않아요. 요청 값을 확인해주세요."),
    INVALID_RESOURCE_PERMISSION_GIVEN(HttpStatus.INTERNAL_SERVER_ERROR, "AUTHORIZATION-0009",
        "권한 확인 요청을 처리하지 못했어요. 관리자에게 문의해주세요."),
    CHALLENGER_ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTHORIZATION-0010", "역할을 찾을 수 없어요. 역할 정보를 확인해주세요."),
    PERMISSION_TYPE_NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, "AUTHORIZATION-0011",
        "아직 지원하지 않는 권한 확인이에요. 관리자에게 문의해주세요."),
    DUPLICATE_CHALLENGER_ROLE(HttpStatus.CONFLICT, "AUTHORIZATION-0012",
        "이미 등록된 역할이에요. 기존 역할을 확인해주세요."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
