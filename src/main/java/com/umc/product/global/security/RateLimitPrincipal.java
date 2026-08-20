package com.umc.product.global.security;

import org.springframework.security.core.AuthenticatedPrincipal;

/**
 * 인증 주체가 API Rate Limiter에 제공하는 안정적인 식별 정보다.
 */
public interface RateLimitPrincipal extends AuthenticatedPrincipal {

    String rateLimitKey();

    String rateLimitClientType();
}
