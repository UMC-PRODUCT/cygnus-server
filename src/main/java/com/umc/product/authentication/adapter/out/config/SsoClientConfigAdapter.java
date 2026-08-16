package com.umc.product.authentication.adapter.out.config;

import org.springframework.stereotype.Component;

import com.umc.product.authentication.application.port.out.LoadSsoClientPort;
import com.umc.product.authentication.config.SsoProperties;
import com.umc.product.authentication.domain.SsoClient;
import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SsoClientConfigAdapter implements LoadSsoClientPort {

    private final SsoProperties properties;

    @Override
    public SsoClient getByClientId(String clientId) {
        SsoProperties.Client client = properties.clients().get(clientId);
        if (client == null) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_SSO_CLIENT);
        }
        return SsoClient.of(
            clientId,
            client.name(),
            client.serviceType(),
            client.environment(),
            client.requirePkce(),
            client.accessTokenTtl(),
            client.redirectUris(),
            client.allowedOrigins()
        );
    }
}
