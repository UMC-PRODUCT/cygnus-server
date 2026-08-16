package com.umc.product.authentication.application.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authentication.application.event.SsoAuthorizationCodeIssuedEvent;
import com.umc.product.authentication.application.port.in.command.AuthorizeSsoUseCase;
import com.umc.product.authentication.application.port.in.command.dto.AuthorizeSsoCommand;
import com.umc.product.authentication.application.port.in.command.dto.SsoAuthorizationRedirectInfo;
import com.umc.product.authentication.application.port.in.dto.SsoBrowserLoginInfo;
import com.umc.product.authentication.application.port.in.query.GetSsoBrowserLoginUseCase;
import com.umc.product.authentication.application.port.out.LoadSsoClientPort;
import com.umc.product.authentication.application.port.out.SaveSsoAuthorizationCodePort;
import com.umc.product.authentication.config.SsoProperties;
import com.umc.product.authentication.domain.PkceChallengeMethod;
import com.umc.product.authentication.domain.SsoAuthorizationCode;
import com.umc.product.authentication.domain.SsoClient;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.global.client.ClientServiceType;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SsoAuthorizationCommandService implements AuthorizeSsoUseCase {

    private static final String AUTHORIZATION_CODE_RESPONSE_TYPE = "code";

    private final LoadSsoClientPort loadSsoClientPort;
    private final SaveSsoAuthorizationCodePort saveSsoAuthorizationCodePort;
    private final GetSsoBrowserLoginUseCase getSsoBrowserLoginUseCase;
    private final SsoProperties properties;
    private final SecureTokenGenerator secureTokenGenerator;
    private final PkceVerifier pkceVerifier;
    private final DomainEventPublisher eventPublisher;

    @Override
    @Transactional
    public SsoAuthorizationRedirectInfo authorize(AuthorizeSsoCommand command) {
        validateResponseType(command.responseType());
        SsoClient client = loadSsoClientPort.getByClientId(command.clientId());
        validateRedirectUri(client, command.redirectUri());
        validateRequestOrigins(client, command.requestOrigins());
        PkceChallengeMethod codeChallengeMethod = validatePkcePolicy(client, command);
        SsoBrowserLoginInfo login = getSsoBrowserLoginUseCase.getLogin(command.rawLoginToken());

        String rawCode = secureTokenGenerator.generateOpaqueToken();
        String codeHash = secureTokenGenerator.sha256Hex(rawCode);
        Instant expiresAt = Instant.now().plus(properties.authorizationCodeTtl());

        SsoAuthorizationCode authorizationCode = SsoAuthorizationCode.create(
            codeHash,
            login.memberId(),
            client.clientId(),
            command.redirectUri(),
            command.codeChallenge(),
            codeChallengeMethod,
            expiresAt
        );

        saveSsoAuthorizationCodePort.save(authorizationCode);
        eventPublisher.publish(SsoAuthorizationCodeIssuedEvent.of(
            login.memberId(),
            client.clientId(),
            command.redirectUri(),
            expiresAt
        ));

        return SsoAuthorizationRedirectInfo.of(command.redirectUri(), rawCode, command.state());
    }

    private void validateResponseType(String responseType) {
        if (!AUTHORIZATION_CODE_RESPONSE_TYPE.equals(responseType)) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_AUTHORIZATION_REQUEST);
        }
    }

    private void validateRedirectUri(SsoClient client, String redirectUri) {
        if (!client.allowsRedirectUri(redirectUri)) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_REDIRECT_URI);
        }
    }

    private void validateRequestOrigins(SsoClient client, List<String> requestOrigins) {
        if (client.allowedOrigins().isEmpty()) {
            if (isNativeAppClient(client)) {
                return;
            }
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_CLIENT);
        }
        // OAuth authorize는 top-level navigation이라 Origin/Referer가 없을 수 있으므로, 값이 있을 때만 등록 origin을 검증한다.
        if (requestOrigins == null || requestOrigins.isEmpty()) {
            return;
        }
        for (String requestOrigin : requestOrigins) {
            if (!client.allowsOrigin(requestOrigin)) {
                throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_AUTHORIZATION_REQUEST);
            }
        }
    }

    private PkceChallengeMethod validatePkcePolicy(SsoClient client, AuthorizeSsoCommand command) {
        if (!client.requirePkce()) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_CLIENT);
        }
        pkceVerifier.requireCodeChallenge(command.codeChallenge());
        return pkceVerifier.requireS256(command.codeChallengeMethod());
    }

    private boolean isNativeAppClient(SsoClient client) {
        return client.serviceType() == ClientServiceType.IOS_APP
            || client.serviceType() == ClientServiceType.ANDROID_APP;
    }
}
