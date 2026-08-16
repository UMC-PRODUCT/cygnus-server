package com.umc.product.global.ratelimit;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.umc.product.global.security.MemberPrincipal;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class RateLimitClientKeyResolver {

    private static final String CLIENT_TYPE_ANONYMOUS = "ANONYMOUS";
    private static final String CLIENT_TYPE_UNKNOWN = "UNKNOWN";

    public RateLimitClientKey resolve(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
            && authentication.isAuthenticated()
            && authentication.getPrincipal() instanceof MemberPrincipal memberPrincipal) {
            return new RateLimitClientKey(
                "member:" + memberPrincipal.getMemberId(),
                true,
                memberPrincipal.getClientType() == null ? CLIENT_TYPE_UNKNOWN : memberPrincipal.getClientType().name()
            );
        }

        return new RateLimitClientKey("ip:" + resolveClientIp(request), false, CLIENT_TYPE_ANONYMOUS);
    }

    private String resolveClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
