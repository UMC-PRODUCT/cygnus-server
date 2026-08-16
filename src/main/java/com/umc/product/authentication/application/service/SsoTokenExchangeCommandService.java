package com.umc.product.authentication.application.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.authentication.application.event.SsoAuthorizationCodeExchangedEvent;
import com.umc.product.authentication.application.event.SsoTokenIssuedEvent;
import com.umc.product.authentication.application.port.in.command.ExchangeSsoAuthorizationCodeUseCase;
import com.umc.product.authentication.application.port.in.command.dto.ExchangeSsoAuthorizationCodeCommand;
import com.umc.product.authentication.application.port.in.command.dto.NewTokens;
import com.umc.product.authentication.application.port.in.command.dto.SsoMemberInfo;
import com.umc.product.authentication.application.port.in.command.dto.SsoTokenInfo;
import com.umc.product.authentication.application.port.in.query.GetMemberOAuthUseCase;
import com.umc.product.authentication.application.port.in.query.dto.MemberOAuthInfo;
import com.umc.product.authentication.application.port.out.LoadSsoAuthorizationCodePort;
import com.umc.product.authentication.application.port.out.LoadSsoClientPort;
import com.umc.product.authentication.domain.SsoAuthorizationCode;
import com.umc.product.authentication.domain.SsoClient;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.common.domain.enums.ClientType;
import com.umc.product.common.domain.enums.OAuthProvider;
import com.umc.product.global.client.ClientContextClaims;
import com.umc.product.global.client.ClientServiceType;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SsoTokenExchangeCommandService implements ExchangeSsoAuthorizationCodeUseCase {

    private static final String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";

    private final LoadSsoAuthorizationCodePort loadSsoAuthorizationCodePort;
    private final LoadSsoClientPort loadSsoClientPort;
    private final SecureTokenGenerator secureTokenGenerator;
    private final PkceVerifier pkceVerifier;
    private final AuthenticationTokenIssuer authenticationTokenIssuer;
    private final GetMemberUseCase getMemberUseCase;
    private final GetMemberOAuthUseCase getMemberOAuthUseCase;
    private final DomainEventPublisher eventPublisher;

    @Override
    @Transactional
    public SsoTokenInfo exchange(ExchangeSsoAuthorizationCodeCommand command) {
        validateGrantType(command.grantType());

        String codeHash = secureTokenGenerator.sha256Hex(command.code());
        SsoAuthorizationCode authorizationCode = loadSsoAuthorizationCodePort.findByCodeHashForUpdate(codeHash)
            .orElseThrow(() -> new AuthenticationDomainException(
                AuthenticationErrorCode.INVALID_SSO_AUTHORIZATION_CODE
            ));

        Instant now = Instant.now();
        validateAuthorizationCodeBeforePkce(authorizationCode, command, now);
        pkceVerifier.verify(command.codeVerifier(), authorizationCode.getCodeChallenge());

        SsoClient client = loadSsoClientPort.getByClientId(command.clientId());
        if (!client.allowsRedirectUri(command.redirectUri())) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_AUTHORIZATION_CODE);
        }

        authorizationCode.consume(command.clientId(), command.redirectUri(), now);

        ClientContextClaims clientContext = ClientContextClaims.of(
            client.clientId(),
            client.serviceType(),
            client.environment()
        );
        NewTokens tokens = authenticationTokenIssuer.issue(
            authorizationCode.getMemberId(),
            toClientType(client.serviceType()),
            clientContext,
            client.accessTokenTtl()
        );
        MemberInfo memberInfo = getMemberUseCase.getById(authorizationCode.getMemberId());
        List<OAuthProvider> linkedOAuthProviders = getMemberOAuthUseCase.getOAuthList(authorizationCode.getMemberId())
            .stream()
            .map(MemberOAuthInfo::provider)
            .toList();

        eventPublisher.publish(SsoAuthorizationCodeExchangedEvent.of(
            authorizationCode.getMemberId(),
            client.clientId(),
            command.redirectUri()
        ));
        eventPublisher.publish(SsoTokenIssuedEvent.of(
            authorizationCode.getMemberId(),
            client.clientId(),
            command.grantType()
        ));

        return SsoTokenInfo.of(
            tokens.accessToken(),
            tokens.refreshToken(),
            tokens.expiresIn(),
            SsoMemberInfo.from(memberInfo),
            linkedOAuthProviders
        );
    }

    private void validateGrantType(String grantType) {
        if (!AUTHORIZATION_CODE_GRANT_TYPE.equals(grantType)) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.UNSUPPORTED_SSO_GRANT_TYPE);
        }
    }

    private ClientType toClientType(ClientServiceType serviceType) {
        if (serviceType == ClientServiceType.IOS_APP) {
            return ClientType.IOS;
        }
        if (serviceType == ClientServiceType.ANDROID_APP) {
            return ClientType.ANDROID;
        }
        return ClientType.WEB;
    }

    private void validateAuthorizationCodeBeforePkce(
        SsoAuthorizationCode authorizationCode,
        ExchangeSsoAuthorizationCodeCommand command,
        Instant now
    ) {
        if (authorizationCode.getUsedAt() != null) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_AUTHORIZATION_CODE);
        }
        if (now == null || !authorizationCode.getExpiresAt().isAfter(now)) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.EXPIRED_SSO_AUTHORIZATION_CODE);
        }
        if (!authorizationCode.getClientId().equals(command.clientId())
            || !authorizationCode.getRedirectUri().equals(command.redirectUri())) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_AUTHORIZATION_CODE);
        }
    }
}
