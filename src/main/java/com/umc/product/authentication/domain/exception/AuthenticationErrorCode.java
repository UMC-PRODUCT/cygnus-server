package com.umc.product.authentication.domain.exception;

import org.springframework.http.HttpStatus;

import com.umc.product.global.response.code.BaseCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthenticationErrorCode implements BaseCode {


    // JWT: JWT 관련 에러
    WRONG_JWT_SIGNATURE(HttpStatus.UNAUTHORIZED, "JWT-0001", "인증 정보가 올바르지 않아요. 다시 로그인해주세요."),
    EXPIRED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "JWT-0002", "로그인이 만료됐어요. 다시 로그인해주세요."),
    UNSUPPORTED_JWT(HttpStatus.UNAUTHORIZED, "JWT-0003", "지원하지 않는 인증 정보예요. 다시 로그인해주세요."),
    INVALID_JWT(HttpStatus.UNAUTHORIZED, "JWT-0004", "인증 정보가 올바르지 않아요. 다시 로그인해주세요."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "JWT-0005", "유효하지 않거나 폐기된 Refresh Token 입니다."),

    // AUTHENTICATION: 인증 관련 에러
    OAUTH_PROVIDER_NOT_FOUND(HttpStatus.BAD_REQUEST, "AUTHENTICATION-0001", "지원하지 않는 로그인 방식이에요. 다른 방식을 선택해주세요."),
    NO_MATCHING_MEMBER(HttpStatus.NOT_FOUND, "AUTHENTICATION-0002",
        "가입된 계정을 찾을 수 없어요. 회원가입을 먼저 진행해주세요."),
    NO_EMAIL_VERIFICATION_METHOD_GIVEN(HttpStatus.BAD_REQUEST, "AUTHENTICATION-0003",
        "이메일 인증 요청이 올바르지 않아요. 인증을 다시 요청해주세요."),
    INVALID_EMAIL_VERIFICATION(HttpStatus.UNAUTHORIZED, "AUTHENTICATION-0004",
        "이메일 인증 정보가 맞지 않아요. 인증 메일을 다시 확인해주세요."),

    // OAUTH 관련 에러
    OAUTH_SUCCESS_BUT_NO_MEMBER(HttpStatus.NOT_FOUND, "AUTHENTICATION-0006",
        "가입된 계정을 찾을 수 없어요. 회원가입을 먼저 진행해주세요."),
    OAUTH_SUCCESS_BUT_NO_INFO(HttpStatus.SERVICE_UNAVAILABLE, "AUTHENTICATION-0007",
        "로그인에 필요한 정보를 받아오지 못했어요. 잠시 후 다시 시도해주세요."),
    OAUTH_FAILURE(HttpStatus.BAD_REQUEST, "AUTHENTICATION-0008", "OAuth 로그인에 실패했어요. 잠시 후 다시 시도해주세요."),
    OAUTH_INVALID_ACCESS_TOKEN(HttpStatus.BAD_REQUEST, "AUTHENTICATION-0009", "OAuth 인증 정보가 올바르지 않아요. 다시 로그인해주세요."),
    OAUTH_TOKEN_VERIFICATION_FAILED(HttpStatus.UNAUTHORIZED, "AUTHENTICATION-0010",
        "OAuth 인증 정보를 확인하지 못했어요. 다시 로그인해주세요."),
    INVALID_OAUTH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTHENTICATION-0011", "OAuth 인증 정보가 올바르지 않아요. 다시 로그인해주세요."),
    OAUTH_ALREADY_LINKED(HttpStatus.UNAUTHORIZED, "AUTHENTICATION-0012", "이미 다른 계정에 연결된 OAuth 계정이에요. 연결된 계정을 확인해주세요."),
    OAUTH_PROVIDER_ALREADY_LINKED(HttpStatus.UNAUTHORIZED, "AUTHENTICATION-0013",
        "이미 연결된 OAuth 제공자예요. 기존 연결을 해제한 뒤 다시 시도해주세요."),
    MEMBER_OAUTH_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTHENTICATION-0014", "연결된 OAuth 정보를 찾을 수 없어요. 다시 연결해주세요."),
    NOT_VALID_MEMBER(HttpStatus.FORBIDDEN, "AUTHENTICATION-0015",
        "이 작업을 할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요."),
    OAUTH_CANNOT_UNLINK_LAST_PROVIDER(HttpStatus.BAD_REQUEST, "AUTHENTICATION-0016",
        "비밀번호를 등록하지 않은 계정은 연결된 유일한 OAuth를 해제할 수 없어요. 비밀번호를 먼저 등록하거나 회원 탈퇴를 이용해주세요."),

    // 이메일 인증 관련
    ALREADY_VERIFIED_EMAIL(HttpStatus.BAD_REQUEST, "AUTHENTICATION-0017",
        "이미 인증이 끝난 이메일 인증 세션이에요. 다음 단계로 진행해주세요."),
    EMAIL_VERIFICATION_SESSION_EXPIRED(HttpStatus.BAD_REQUEST, "AUTHENTICATION-0018",
        "이메일 인증 세션이 만료됐어요. 새로운 인증을 요청해주세요."),
    EMAIL_VERIFICATION_THROTTLED(HttpStatus.TOO_MANY_REQUESTS, "AUTHENTICATION-0027",
        "이메일 인증 요청이 너무 잦아요. 잠시 후 다시 시도해주세요."),

    // ID/PW 자격증명 관련 에러
    LOGIN_ID_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTHENTICATION-0019", "이미 사용 중인 로그인 ID예요. 다른 ID를 입력해주세요."),
    INVALID_LOGIN_ID_FORMAT(HttpStatus.BAD_REQUEST, "AUTHENTICATION-0020",
        "로그인 ID는 영문, 숫자, ., _, -를 사용해 5~20자로 입력해주세요."),
    PASSWORD_POLICY_VIOLATION(HttpStatus.BAD_REQUEST, "AUTHENTICATION-0021",
        "비밀번호는 8~64자로 입력하고 영문, 숫자, 특수문자 중 2종류 이상을 포함해주세요."),
    // ID 존재 여부 / 비밀번호 오류 / 자격증명 미등록 등을 외부에 구분 노출하지 않기 위한 단일 에러
    INVALID_LOGIN_CREDENTIAL(HttpStatus.UNAUTHORIZED, "AUTHENTICATION-0022",
        "로그인 ID 또는 비밀번호가 올바르지 않아요. 다시 입력해주세요."),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "AUTHENTICATION-0025",
        "이메일 형식이 올바르지 않아요. 이메일 주소를 확인해주세요."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTHENTICATION-0026",
        "이미 사용 중인 이메일이에요. 다른 이메일을 입력해주세요."),
    INVALID_SSO_CLIENT(HttpStatus.BAD_REQUEST, "AUTHENTICATION-0028",
        "지원하지 않는 SSO client예요. 설정을 확인해주세요."),
    INVALID_SSO_REDIRECT_URI(HttpStatus.BAD_REQUEST, "AUTHENTICATION-0029",
        "허용되지 않은 SSO redirect URI예요. 설정을 확인해주세요."),
    INVALID_SSO_AUTHORIZATION_REQUEST(HttpStatus.BAD_REQUEST, "AUTHENTICATION-0030", "SSO 인증 요청이 올바르지 않아요."),
    INVALID_SSO_PKCE(HttpStatus.BAD_REQUEST, "AUTHENTICATION-0031", "SSO PKCE 검증에 실패했어요."),
    SSO_BROWSER_LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTHENTICATION-0032", "Auth App 로그인이 필요해요."),
    INVALID_SSO_BROWSER_LOGIN(HttpStatus.UNAUTHORIZED, "AUTHENTICATION-0033", "Auth App 로그인 정보가 유효하지 않아요."),
    INVALID_SSO_AUTHORIZATION_CODE(HttpStatus.UNAUTHORIZED, "AUTHENTICATION-0034", "SSO authorization code가 유효하지 않아요."),
    EXPIRED_SSO_AUTHORIZATION_CODE(HttpStatus.UNAUTHORIZED, "AUTHENTICATION-0035", "SSO authorization code가 만료됐어요."),
    UNSUPPORTED_SSO_GRANT_TYPE(HttpStatus.BAD_REQUEST, "AUTHENTICATION-0036", "지원하지 않는 SSO grant_type이에요."),

    // OAuth Authorization Code Flow 관련 에러
    UNSUPPORTED_OAUTH_FLOW(HttpStatus.BAD_REQUEST, "AUTHENTICATION-0023",
        "선택한 OAuth 제공자는 이 인증 방식을 지원하지 않아요. 다른 로그인 방식을 사용해주세요."),
    INVALID_OAUTH_REDIRECT_URI(HttpStatus.BAD_REQUEST, "AUTHENTICATION-0024",
        "허용되지 않은 OAuth redirect URI예요. 설정을 확인해주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
