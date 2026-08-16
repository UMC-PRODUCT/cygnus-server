package com.umc.product.authentication.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.member.application.port.in.query.GetMemberCredentialUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberCredentialInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("SsoCredentialVerifier")
class SsoCredentialVerifierTest {

    private static final Long MEMBER_ID = 1L;
    private static final String EMAIL = "alice@example.com";
    private static final String RAW_PASSWORD = "Strong-Pw-2026";
    private static final String PASSWORD_HASH = "{argon2}$argon2id$v=19$m=16384,t=2,p=1$abc$def";

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    GetMemberCredentialUseCase getMemberCredentialUseCase;

    @Mock
    CredentialRehashService rehashService;

    @InjectMocks
    SsoCredentialVerifier verifier;

    @Test
    @DisplayName("email/password 검증 성공 시 rehash를 확인하고 memberId를 반환한다")
    void 이메일_비밀번호_검증_성공() {
        // given
        MemberCredentialInfo credential = new MemberCredentialInfo(MEMBER_ID, PASSWORD_HASH);
        given(getMemberCredentialUseCase.findCredentialByEmail(EMAIL)).willReturn(Optional.of(credential));
        given(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).willReturn(true);

        // when
        Long memberId = verifier.verifyEmailPassword(EMAIL, RAW_PASSWORD);

        // then
        assertThat(memberId).isEqualTo(MEMBER_ID);
        then(getMemberCredentialUseCase).should().findCredentialByEmail(EMAIL);
        then(passwordEncoder).should().matches(RAW_PASSWORD, PASSWORD_HASH);
        then(rehashService).should().rehashIfNeeded(credential, RAW_PASSWORD);
    }

    @Test
    @DisplayName("자격증명이 없으면 INVALID_LOGIN_CREDENTIAL 예외를 던지고 비밀번호 검증과 rehash를 호출하지 않는다")
    void 자격증명_없음_거부() {
        // given
        given(getMemberCredentialUseCase.findCredentialByEmail(EMAIL)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> verifier.verifyEmailPassword(EMAIL, RAW_PASSWORD))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL);

        then(passwordEncoder).should(never()).matches(anyString(), anyString());
        then(rehashService).should(never()).rehashIfNeeded(any(), anyString());
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 INVALID_LOGIN_CREDENTIAL 예외를 던지고 rehash를 호출하지 않는다")
    void 비밀번호_불일치_거부() {
        // given
        MemberCredentialInfo credential = new MemberCredentialInfo(MEMBER_ID, PASSWORD_HASH);
        given(getMemberCredentialUseCase.findCredentialByEmail(EMAIL)).willReturn(Optional.of(credential));
        given(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> verifier.verifyEmailPassword(EMAIL, RAW_PASSWORD))
            .isInstanceOf(AuthenticationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL);

        then(rehashService).should(never()).rehashIfNeeded(any(), anyString());
    }
}
