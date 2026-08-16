package com.umc.product.global.exception.constant;


import org.springframework.http.HttpStatus;

import com.umc.product.global.response.code.BaseCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 프로젝트 전체에서 공통으로 사용되는 COMMON 카테고리의 에러입니다.
 */
@Getter
@AllArgsConstructor
public enum CommonErrorCode implements BaseCode {

    // Error Code는 DOMAIN-CATEGORY-NUMBER 형식으로 작성할 것.
    // e.g. CHALLENGER-COMMON-0001
    // 카테고리는 도메인 내의 세부 카테고리, 작성자에게 권한을 드립니다.
    // Number는 0001 부터 4자리로 작성하며, 삭제할 경우 결번 처리하여 중복을 방지해주세요.
    // 반드시 Notion 및 Docs와 동기화해주세요.
    // 에러 코드는 되도록 재사용 금지

    // COMMON: 일반 상태 코드
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-0001",
        "요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON-400", "요청 값이 올바르지 않아요. 입력한 값을 확인해주세요."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON-401", "로그인이 필요해요. 로그인 후 다시 시도해주세요."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON-403", "요청할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON-404", "요청한 항목을 찾을 수 없어요. 입력한 값을 확인해주세요."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "COMMON-429", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, "COMMON-501", "아직 사용할 수 없는 기능이에요. 필요한 기능이라면 서버팀에 문의해주세요."),

    // SECURITY: Spring Security에서 발생하는 에러
    SECURITY_NOT_GIVEN(HttpStatus.UNAUTHORIZED, "SECURITY-0001", "인증 정보가 없어요. 로그인 후 다시 시도해주세요."),
    SECURITY_FORBIDDEN(HttpStatus.FORBIDDEN, "SECURITY-0002", "권한이 부족해요. 필요한 권한이 있다면 운영진에게 문의해주세요."),
    SECURITY_WEBSOCKET_BROKER_ACCESS(HttpStatus.FORBIDDEN, "SECURITY-0003", "브로커 경로로 직접 메시지를 전송할 수 없습니다."),
    SECURITY_WEBSOCKET_INVALID_DESTINATION(HttpStatus.FORBIDDEN, "SECURITY-0004", "허용되지 않은 웹소켓 경로입니다."),

    // ENVIRONMENT: SpringBoot 실행환경 관련 에러
    INVALID_ENV(HttpStatus.BAD_REQUEST, "ENV-0001", "현재 실행 환경에서는 사용할 수 없는 기능이에요. 환경 설정을 확인해주세요."),

    // Permission Evaluator 관련 에러
    PERMISSION_TYPE_NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, "PE-0001", "아직 지원하지 않는 권한 확인이에요. 관리자에게 문의해주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
