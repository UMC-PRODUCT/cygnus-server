package com.umc.product.global.ratelimit;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class RateLimitRouteResolver {

    public static final String UNMAPPED_ROUTE_PATTERN = "unmapped";

    public String resolve(HttpServletRequest request) {
        Object bestMatchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (bestMatchingPattern instanceof String pattern && !pattern.isBlank()) {
            return pattern;
        }
        return UNMAPPED_ROUTE_PATTERN;
    }
}
