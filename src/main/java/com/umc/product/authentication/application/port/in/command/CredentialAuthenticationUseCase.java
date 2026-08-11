package com.umc.product.authentication.application.port.in.command;

import com.umc.product.authentication.application.port.in.command.dto.ChangePasswordCommand;
import com.umc.product.authentication.application.port.in.command.dto.LocalLoginResult;
import com.umc.product.authentication.application.port.in.command.dto.LoginByEmailCommand;
import com.umc.product.authentication.application.port.in.command.dto.RegisterCredentialByEmailCommand;
import com.umc.product.authentication.application.port.in.command.dto.ResetPasswordByEmailCommand;
import com.umc.product.authentication.application.port.in.command.dto.ResetPasswordByMemberIdCommand;

/**
 * 이메일/PW 자격증명 등록/변경/로그인 UseCase. ADR-017 흐름.
 * <p>
 * 평문 비밀번호 검증과 인코딩 책임은 Auth 도메인이 가진다.
 * Member 도메인에는 이미 인코딩된 해시만 전달된다.
 */
public interface CredentialAuthenticationUseCase {

    /**
     * 회원에게 이메일 기반 자격증명(비밀번호)을 최초 등록한다.
     * <p>
     * email 은 이미 회원 생성 단계에서 emailVerificationToken 으로 검증되어
     * Member.email 에 저장되어 있다.
     */
    void registerCredentialByEmail(RegisterCredentialByEmailCommand command);

    /** 현재 비밀번호 검증 후 신규 비밀번호로 교체한다. */
    void changePassword(ChangePasswordCommand command);

    /**
     * 이메일 인증을 통한 비밀번호 초기화. 현재 비밀번호 없이 새 비밀번호로 교체한다.
     * <p>
     * email 은 emailVerificationToken 으로 사전 검증된 값이며, 해당 이메일로 가입된 회원이
     * 자격증명을 보유한 경우에만 동작한다. 그렇지 않으면 사용자 열거 방지를 위해
     * {@code INVALID_LOGIN_CREDENTIAL} 단일 메시지로 응답한다.
     */
    void resetPasswordByEmail(ResetPasswordByEmailCommand command);

    /** 내부 관리자 발급 계정의 비밀번호를 회원 ID 기준으로 초기화한다. */
    void resetPasswordByMemberId(ResetPasswordByMemberIdCommand command);

    /**
     * 이메일/PW 로그인. 성공 시 JWT 토큰 쌍을 발급하며,
     * 해시 파라미터가 갱신 대상이면 transparent rehash 를 수행한다.
     */
    LocalLoginResult loginByEmail(LoginByEmailCommand command);
}
