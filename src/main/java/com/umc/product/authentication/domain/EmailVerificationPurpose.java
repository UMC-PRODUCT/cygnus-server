package com.umc.product.authentication.domain;

/**
 * 이메일 인증 세션 및 emailVerificationToken 의 용도를 구분한다.
 * <p>
 * 회원가입용과 비밀번호 초기화용 토큰을 분리해, 한 흐름에서 발급된 토큰이
 * 다른 흐름에 재사용되지 않도록 한다 (cross-purpose 공격 방어).
 */
public enum EmailVerificationPurpose {
    /**
     * 신규 회원가입을 위한 이메일 인증.
     */
    REGISTER,

    /**
     * 가입된 회원의 비밀번호 초기화를 위한 이메일 인증.
     */
    PASSWORD_RESET,

    /**
     * 기존 회원의 이메일 변경을 위한 새 이메일 소유 인증.
     */
    CHANGE_EMAIL
}
