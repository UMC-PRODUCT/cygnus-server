package com.umc.product.global.ratelimit;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.umc.product.global.security.RateLimitPrincipal;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class RateLimitClientKeyResolver {

    private static final String CLIENT_TYPE_ANONYMOUS = "ANONYMOUS";

    public RateLimitClientKey resolve(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
            && authentication.isAuthenticated()
            && authentication.getPrincipal() instanceof RateLimitPrincipal principal) {
            return new RateLimitClientKey(
                principal.rateLimitKey(),
                true,
                principal.rateLimitClientType()
            );
        }

        return new RateLimitClientKey("ip:" + resolveClientIp(request), false, CLIENT_TYPE_ANONYMOUS);
    }

    private String resolveClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
