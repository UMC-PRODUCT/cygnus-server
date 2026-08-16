package com.umc.product.authentication.application.service;

import java.time.Instant;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authentication.application.event.SsoBrowserLoginCreatedEvent;
import com.umc.product.authentication.application.port.in.command.ManageSsoBrowserLoginUseCase;
import com.umc.product.authentication.application.port.in.command.OAuthAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.dto.AccessTokenLoginCommand;
import com.umc.product.authentication.application.port.in.command.dto.LoginSsoBrowserByAppleAuthorizationCodeCommand;
import com.umc.product.authentication.application.port.in.command.dto.LoginSsoBrowserByEmailCommand;
import com.umc.product.authentication.application.port.in.command.dto.LoginSsoBrowserByOAuthTokenCommand;
import com.umc.product.authentication.application.port.in.command.dto.OAuthTokenLoginResult;
import com.umc.product.authentication.application.port.in.dto.SsoBrowserLoginInfo;
import com.umc.product.authentication.application.port.in.dto.SsoBrowserOAuthLoginResult;
import com.umc.product.authentication.application.port.out.AppleAuthorizationCodeResult;
import com.umc.product.authentication.application.port.out.VerifyOAuthTokenPort;
import com.umc.product.authentication.config.SsoProperties;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SsoBrowserLoginCommandService implements ManageSsoBrowserLoginUseCase {

    private static final String EMAIL_AUTHENTICATION_METHOD = "email";

    private final SsoCredentialVerifier credentialVerifier;
    private final SsoLoginTokenProvider tokenProvider;
    private final SsoProperties properties;
    private final DomainEventPublisher eventPublisher;
    private final OAuthAuthenticationUseCase oAuthAuthenticationUseCase;
    private final VerifyOAuthTokenPort verifyOAuthTokenPort;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public SsoBrowserLoginInfo loginByEmail(LoginSsoBrowserByEmailCommand command) {
        Long memberId = credentialVerifier.verifyEmailPassword(command.email(), command.rawPassword());
        return issueLogin(memberId, EMAIL_AUTHENTICATION_METHOD);
    }

    @Override
    @Transactional
    public SsoBrowserOAuthLoginResult loginByOAuthToken(LoginSsoBrowserByOAuthTokenCommand command) {
        OAuthTokenLoginResult result = oAuthAuthenticationUseCase.accessTokenLogin(
            AccessTokenLoginCommand.of(command.provider(), command.token())
        );
        return buildOAuthLoginResult(result);
    }

    @Override
    @Transactional
    public SsoBrowserOAuthLoginResult loginByAppleAuthorizationCode(
        LoginSsoBrowserByAppleAuthorizationCodeCommand command
    ) {
        AppleAuthorizationCodeResult codeResult = verifyOAuthTokenPort.verifyAppleAuthorizationCode(
            command.authorizationCode(),
            ClientType.WEB
        );
        OAuthTokenLoginResult result = oAuthAuthenticationUseCase.loginWithOAuthAttributes(codeResult.attrs());

        if (result.isExistingMember() && codeResult.refreshToken() != null) {
            oAuthAuthenticationUseCase.updateAppleRefreshToken(
                OAuthProvider.APPLE,
                result.providerId(),
                codeResult.refreshToken(),
                codeResult.clientId()
            );
        }

        return buildOAuthLoginResult(result);
    }

    private SsoBrowserOAuthLoginResult buildOAuthLoginResult(OAuthTokenLoginResult result) {
        if (!result.isExistingMember()) {
            String verificationToken = jwtTokenProvider.createOAuthVerificationToken(
                result.email(),
                result.provider(),
                result.providerId()
            );
            return SsoBrowserOAuthLoginResult.registerRequired(result.provider(), verificationToken);
        }

        String authenticationMethod = result.provider().name().toLowerCase(Locale.ROOT);
        SsoBrowserLoginInfo loginInfo = issueLogin(result.memberId(), authenticationMethod);
        return SsoBrowserOAuthLoginResult.loginSuccess(result.provider(), loginInfo);
    }

    private SsoBrowserLoginInfo issueLogin(Long memberId, String authenticationMethod) {
        Instant expiresAt = Instant.now().plus(properties.browserLoginTtl());
        String loginToken = tokenProvider.createLoginToken(memberId, authenticationMethod, expiresAt);

        eventPublisher.publish(SsoBrowserLoginCreatedEvent.of(memberId, authenticationMethod));
        return SsoBrowserLoginInfo.of(memberId, loginToken, expiresAt);
    }
}
