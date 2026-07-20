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

import com.umc.product.authentication.adapter.in.web.dto.request.CompleteEmailVerificationRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.ResendEmailVerificationRequest;
import com.umc.product.authentication.adapter.in.web.dto.request.SendEmailVerificationRequest;
import com.umc.product.authentication.adapter.in.web.dto.response.CompleteEmailVerificationResponse;
import com.umc.product.authentication.adapter.in.web.dto.response.SendEmailVerificationResponse;
import com.umc.product.authentication.application.port.in.command.ManageAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.OAuthAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.dto.ValidateEmailVerificationSessionCommand;
import com.umc.product.authentication.application.port.out.VerifyOAuthTokenPort;
import com.umc.product.authentication.domain.EmailVerificationPurpose;
import com.umc.product.global.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailAuthenticationController")
class EmailAuthenticationControllerUnitTest {

    @Mock
    ManageAuthenticationUseCase manageAuthenticationUseCase;
    @Mock
    OAuthAuthenticationUseCase oAuthAuthenticationUseCase;
    @Mock
    VerifyOAuthTokenPort verifyOAuthTokenPort;
    @Mock
    JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    EmailAuthenticationController sut;

    @Test
    @DisplayName("인증 ID와 code를 검증해 이메일 인증 토큰을 응답한다")
    void 이메일_code를_검증한다() {
        ValidateEmailVerificationSessionCommand command = ValidateEmailVerificationSessionCommand.builder()
            .sessionId(10L)
            .code("123456")
            .build();
        given(manageAuthenticationUseCase.validateEmailVerificationSession(command))
            .willReturn("email-verification-token");

        CompleteEmailVerificationResponse result = sut.verifyEmailByCode(
            new CompleteEmailVerificationRequest(10L, "123456"));

        assertThat(result.emailVerificationToken()).isEqualTo("email-verification-token");
    }

    @Test
    @DisplayName("이메일과 인증 목적을 고정한 세션 ID를 응답한다")
    void 이메일_인증_세션을_생성한다() {
        given(manageAuthenticationUseCase.createEmailVerificationSession(
            "member@example.com", EmailVerificationPurpose.PASSWORD_RESET))
            .willReturn(10L);

        SendEmailVerificationResponse result = sut.sendEmailVerification(
            new SendEmailVerificationRequest("member@example.com", EmailVerificationPurpose.PASSWORD_RESET));

        assertThat(result.emailVerificationId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("재전송 요청은 기존 이메일 인증 세션 ID를 그대로 위임한다")
    void 이메일_인증을_재전송한다() {
        sut.resendEmailVerification(new ResendEmailVerificationRequest(10L));

        then(manageAuthenticationUseCase).should().resendEmailVerification(10L);
    }
}
