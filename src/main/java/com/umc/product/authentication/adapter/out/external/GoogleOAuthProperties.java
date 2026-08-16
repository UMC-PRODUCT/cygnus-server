package com.umc.product.authentication.adapter.out.external;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.oauth2.google")
public record GoogleOAuthProperties(
    List<String> clientIdList,
    OidcJwksCacheProperties jwksCache
) {

    public GoogleOAuthProperties {
        if (clientIdList == null) {
            clientIdList = List.of();
        } else {
            clientIdList = clientIdList.stream()
                .filter(StringUtils::hasText)
                .toList();
        }
        if (jwksCache == null) {
            jwksCache = OidcJwksCacheProperties.defaults();
        }
    }
}
