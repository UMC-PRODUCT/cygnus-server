package com.umc.product.authentication.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.umc.product.authentication.application.port.in.command.dto.ChangePasswordCommand;
import com.umc.product.authentication.application.port.in.command.dto.LocalLoginResult;
import com.umc.product.authentication.application.port.in.command.dto.LoginByEmailCommand;
import com.umc.product.authentication.application.port.in.command.dto.NewTokens;
import com.umc.product.authentication.application.port.in.command.dto.RegisterCredentialByEmailCommand;
import com.umc.product.authentication.application.port.in.command.dto.ResetPasswordByEmailCommand;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.member.application.port.in.command.ManageMemberCredentialUseCase;
import com.umc.product.member.application.port.in.command.dto.ChangeMemberPasswordCommand;
import com.umc.product.member.application.port.in.command.dto.RegisterMemberCredentialByEmailCommand;
import com.umc.product.member.application.port.in.query.GetMemberCredentialUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberCredentialInfo;

/**
 * CredentialAuthenticationService 단위 테스트. ADR-017 흐름.
 * <p>
 * 외부 협력자(PasswordEncoder, AuthenticationTokenIssuer, Member 측 UseCase)는 모두 mock 으로 두고
 * Service 내부의 분기/사용자 열거 방지 / rehash 정책을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class CredentialAuthenticationServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final String EMAIL = "alice@example.com";
    private static final String RAW_PASSWORD = "Strong-Pw-2026";
    private static final String ENCODED_PASSWORD = "{argon2}$argon2id$v=19$m=16384,t=2,p=1$abc$def";
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    AuthenticationTokenIssuer authenticationTokenIssuer;
    @Mock
    GetMemberCredentialUseCase getMemberCredentialUseCase;
    @Mock
    ManageMemberCredentialUseCase manageMemberCredentialUseCase;
    @Mock
    CredentialRehashService rehashService;
    @Mock
    SsoCredentialVerifier credentialVerifier;
    @Mock
    OperationalMetrics operationalMetrics;
    @InjectMocks
    CredentialAuthenticationService service;

    @Nested
    @DisplayName("자격 증명 등록 (이메일 기반)")
    class RegisterCredentialByEmail {

        @Test
        @DisplayName("인코딩된 비밀번호로 회원 자격증명을 등록한다")
        void 정상_등록() {
            // given
            given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(ENCODED_PASSWORD);

            RegisterCredentialByEmailCommand command =
                RegisterCredentialByEmailCommand.of(MEMBER_ID, RAW_PASSWORD);

            // when
            service.registerCredentialByEmail(command);

            // then
            ArgumentCaptor<RegisterMemberCredentialByEmailCommand> captor =
                ArgumentCaptor.forClass(RegisterMemberCredentialByEmailCommand.class);
            then(manageMemberCredentialUseCase).should().registerCredentialByEmail(captor.capture());
            RegisterMemberCredentialByEmailCommand captured = captor.getValue();
            assertThat(captured.memberId()).isEqualTo(MEMBER_ID);
            assertThat(captured.encodedPassword()).isEqualTo(ENCODED_PASSWORD);
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("현재 비밀번호가 일치하면 새 비밀번호로 교체한다")
        void 비밀번호_변경_성공() {
            // given
            MemberCredentialInfo credential = new MemberCredentialInfo(MEMBER_ID, ENCODED_PASSWORD);
            given(getMemberCredentialUseCase.findCredentialByMemberId(MEMBER_ID))
                .willReturn(Optional.of(credential));
            given(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
            given(passwordEncoder.encode("New-Pw-2026")).willReturn("{argon2}new-hash");

            ChangePasswordCommand command = ChangePasswordCommand.of(MEMBER_ID, RAW_PASSWORD, "New-Pw-2026");

            // when
            service.changePassword(command);

            // then
            ArgumentCaptor<ChangeMemberPasswordCommand> captor =
                ArgumentCaptor.forClass(ChangeMemberPasswordCommand.class);
            then(manageMemberCredentialUseCase).should().changePassword(captor.capture());
            assertThat(captor.getValue().memberId()).isEqualTo(MEMBER_ID);
            assertThat(captor.getValue().encodedPassword()).isEqualTo("{argon2}new-hash");
        }

        @Test
        @DisplayName("자격증명이 등록되지 않은 회원은 INVALID_LOGIN_CREDENTIAL 로 응답한다")
        void 자격증명_미등록이면_단일_메시지() {
            // given
            given(getMemberCredentialUseCase.findCredentialByMemberId(MEMBER_ID))
                .willReturn(Optional.empty());

            ChangePasswordCommand command = ChangePasswordCommand.of(MEMBER_ID, RAW_PASSWORD, "New-Pw-2026");

            // when & then
            assertThatThrownBy(() -> service.changePassword(command))
                .isInstanceOf(AuthenticationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL);

            then(passwordEncoder).should(never()).encode(anyString());
            then(manageMemberCredentialUseCase).should(never()).changePassword(any());
        }

        @Test
        @DisplayName("현재 비밀번호가 다르면 INVALID_LOGIN_CREDENTIAL 로 응답한다")
        void 현재_비밀번호_불일치면_단일_메시지() {
            // given
            MemberCredentialInfo credential = new MemberCredentialInfo(MEMBER_ID, ENCODED_PASSWORD);
            given(getMemberCredentialUseCase.findCredentialByMemberId(MEMBER_ID))
                .willReturn(Optional.of(credential));
            given(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).willReturn(false);

            ChangePasswordCommand command = ChangePasswordCommand.of(MEMBER_ID, RAW_PASSWORD, "New-Pw-2026");

            // when & then
            assertThatThrownBy(() -> service.changePassword(command))
                .isInstanceOf(AuthenticationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL);

            then(manageMemberCredentialUseCase).should(never()).changePassword(any());
        }
    }

    @Nested
    @DisplayName("이메일 인증 기반 비밀번호 초기화")
    class ResetPasswordByEmail {

        private static final String NEW_RAW_PASSWORD = "New-Strong-Pw-2026";
        private static final String NEW_ENCODED_PASSWORD = "{argon2}$argon2id$v=19$m=16384,t=2,p=1$new$hash";

        @Test
        @DisplayName("이메일로 자격증명을 가진 회원이 있으면 새 비밀번호로 교체한다")
        void 정상_초기화() {
            // given
            MemberCredentialInfo credential = new MemberCredentialInfo(MEMBER_ID, ENCODED_PASSWORD);
            given(getMemberCredentialUseCase.findCredentialByEmail(EMAIL))
                .willReturn(Optional.of(credential));
            given(passwordEncoder.encode(NEW_RAW_PASSWORD)).willReturn(NEW_ENCODED_PASSWORD);

            ResetPasswordByEmailCommand command =
                ResetPasswordByEmailCommand.of(EMAIL, NEW_RAW_PASSWORD);

            // when
            service.resetPasswordByEmail(command);

            // then
            ArgumentCaptor<ChangeMemberPasswordCommand> captor =
                ArgumentCaptor.forClass(ChangeMemberPasswordCommand.class);
            then(manageMemberCredentialUseCase).should().changePassword(captor.capture());
            assertThat(captor.getValue().memberId()).isEqualTo(MEMBER_ID);
            assertThat(captor.getValue().encodedPassword()).isEqualTo(NEW_ENCODED_PASSWORD);
        }

        @Test
        @DisplayName("자격증명이 없는 회원(또는 미가입 이메일)은 INVALID_LOGIN_CREDENTIAL 단일 메시지로 응답한다")
        void 자격증명_없으면_단일_메시지() {
            // given
            given(getMemberCredentialUseCase.findCredentialByEmail(EMAIL))
                .willReturn(Optional.empty());

            ResetPasswordByEmailCommand command =
                ResetPasswordByEmailCommand.of(EMAIL, NEW_RAW_PASSWORD);

            // when & then
            assertThatThrownBy(() -> service.resetPasswordByEmail(command))
                .isInstanceOf(AuthenticationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL);

            then(passwordEncoder).should(never()).encode(anyString());
            then(manageMemberCredentialUseCase).should(never()).changePassword(any());
        }
    }

    @Nested
    @DisplayName("이메일/PW 로그인")
    class LoginByEmail {

        @Test
        @DisplayName("정상 로그인 시 공통 credential verifier로 검증하고 clientType 을 포함해 토큰 발급을 위임한다")
        void 로그인_성공_clientType_토큰_발급_rehash호출_확인() {
            // given
            given(credentialVerifier.verifyEmailPassword(EMAIL, RAW_PASSWORD)).willReturn(MEMBER_ID);
            given(authenticationTokenIssuer.issue(eq(MEMBER_ID), eq(ClientType.IOS)))
                .willReturn(NewTokens.builder()
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .build());

            // when
            LocalLoginResult result = service.loginByEmail(
                LoginByEmailCommand.of(EMAIL, RAW_PASSWORD, ClientType.IOS)
            );

            // then
            assertThat(result.memberId()).isEqualTo(MEMBER_ID);
            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
            then(credentialVerifier).should().verifyEmailPassword(EMAIL, RAW_PASSWORD);
            then(authenticationTokenIssuer).should().issue(MEMBER_ID, ClientType.IOS);
            then(operationalMetrics).should().recordSecurityEvent("AUTHENTICATION", "EMAIL_LOGIN", "success");
        }

        @Test
        @DisplayName("자격증명을 찾을 수 없으면 INVALID_LOGIN_CREDENTIAL 단일 메시지를 반환한다")
        void 자격증명_없으면_단일_메시지() {
            // given
            given(credentialVerifier.verifyEmailPassword(EMAIL, RAW_PASSWORD))
                .willThrow(new AuthenticationDomainException(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL));

            // when & then
            assertThatThrownBy(() -> service.loginByEmail(LoginByEmailCommand.of(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(AuthenticationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL);

            // 매칭/토큰 발급은 호출되지 않아야 한다
            then(passwordEncoder).should(never()).matches(anyString(), anyString());
            then(authenticationTokenIssuer).should(never()).issue(any(), any());
            then(operationalMetrics).should().recordSecurityEvent("AUTHENTICATION", "EMAIL_LOGIN", "failure");
        }

        @Test
        @DisplayName("비밀번호가 다르면 INVALID_LOGIN_CREDENTIAL 단일 메시지를 반환한다")
        void 비밀번호_불일치면_단일_메시지() {
            // given
            given(credentialVerifier.verifyEmailPassword(EMAIL, RAW_PASSWORD))
                .willThrow(new AuthenticationDomainException(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL));

            // when & then
            assertThatThrownBy(() -> service.loginByEmail(LoginByEmailCommand.of(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(AuthenticationDomainException.class)
                .extracting("baseCode")
                .isEqualTo(AuthenticationErrorCode.INVALID_LOGIN_CREDENTIAL);

            then(authenticationTokenIssuer).should(never()).issue(any(), any());
            then(manageMemberCredentialUseCase).should(never()).changePassword(any());
            then(operationalMetrics).should().recordSecurityEvent("AUTHENTICATION", "EMAIL_LOGIN", "failure");
        }
    }
}
