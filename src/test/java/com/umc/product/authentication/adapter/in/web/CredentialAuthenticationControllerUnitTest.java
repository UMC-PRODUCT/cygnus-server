package com.umc.product.authentication.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authentication.adapter.in.web.dto.request.ChangePasswordRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.LoginByEmailRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.RegisterCredentialRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.ResetPasswordByEmailRequest;
import com.umc.product.authentication.adapter.in.web.dto.response.EmailAvailabilityResponse;
import com.umc.product.authentication.adapter.in.web.dto.response.LocalLoginResponse;
import com.umc.product.authentication.application.port.in.command.CredentialAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.dto.ChangePasswordCommand;
import com.umc.product.authentication.application.port.in.command.dto.LocalLoginResult;
import com.umc.product.authentication.application.port.in.command.dto.LoginByEmailCommand;
import com.umc.product.authentication.application.port.in.command.dto.RegisterCredentialByEmailCommand;
import com.umc.product.authentication.application.port.in.command.dto.ResetPasswordByEmailCommand;
import com.umc.product.authentication.application.port.in.query.CheckCredentialAvailabilityUseCase;
import com.umc.product.authentication.domain.EmailVerificationPurpose;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.MemberPrincipal;

@ExtendWith(MockitoExtension.class)
@DisplayName("CredentialAuthenticationController")
class CredentialAuthenticationControllerUnitTest {

    @Mock
    CredentialAuthenticationUseCase credentialAuthenticationUseCase;
    @Mock
    CheckCredentialAvailabilityUseCase checkCredentialAvailabilityUseCase;
    @Mock
    JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    CredentialAuthenticationController sut;

    @Test
    @DisplayName("현재 회원에게 비밀번호 자격증명을 등록한다")
    void 자격증명을_등록한다() {
        sut.registerCredential(new MemberPrincipal(1L), new RegisterCredentialRequest("Password1!"));

        then(credentialAuthenticationUseCase).should().registerCredentialByEmail(
            RegisterCredentialByEmailCommand.of(1L, "Password1!"));
    }

    @Test
    @DisplayName("현재 비밀번호와 새 비밀번호를 사용해 비밀번호를 변경한다")
    void 비밀번호를_변경한다() {
        sut.changePassword(
            new MemberPrincipal(1L),
            new ChangePasswordRequest("Current1!", "Changed1!")
        );

        then(credentialAuthenticationUseCase).should().changePassword(
            ChangePasswordCommand.of(1L, "Current1!", "Changed1!"));
    }

    @Test
    @DisplayName("PASSWORD_RESET 용도의 이메일 인증 토큰으로 비밀번호를 초기화한다")
    void 이메일로_비밀번호를_초기화한다() {
        given(jwtTokenProvider.parseEmailVerificationToken(
            "email-token", EmailVerificationPurpose.PASSWORD_RESET))
            .willReturn("member@example.com");

        sut.resetPasswordByEmail(new ResetPasswordByEmailRequest("email-token", "Changed1!"));

        then(credentialAuthenticationUseCase).should().resetPasswordByEmail(
            ResetPasswordByEmailCommand.of("member@example.com", "Changed1!"));
    }

    @Test
    @DisplayName("이메일 사용 가능 여부에 원본 이메일을 함께 응답한다")
    void 이메일_사용_가능성을_조회한다() {
        given(checkCredentialAvailabilityUseCase.isEmailAvailable("new@example.com")).willReturn(true);

        EmailAvailabilityResponse result = sut.checkEmailAvailability("new@example.com");

        assertThat(result.email()).isEqualTo("new@example.com");
        assertThat(result.available()).isTrue();
    }

    @Test
    @DisplayName("이메일 로그인 결과의 회원 ID와 두 토큰을 응답한다")
    void 이메일로_로그인한다() {
        LoginByEmailCommand command = LoginByEmailCommand.of(
            "member@example.com", "Password1!", ClientType.WEB);
        given(credentialAuthenticationUseCase.loginByEmail(command))
            .willReturn(LocalLoginResult.builder()
                .memberId(1L)
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build());

        LocalLoginResponse result = sut.loginByEmail(
            new LoginByEmailRequest("member@example.com", "Password1!", ClientType.WEB));

        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }
}
