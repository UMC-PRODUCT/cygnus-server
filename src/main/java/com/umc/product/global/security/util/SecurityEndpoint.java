package com.umc.product.global.security.util;

import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

public record SecurityEndpoint(HttpMethod method, String pattern) {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    public static SecurityEndpoint any(String pattern) {
        return new SecurityEndpoint(null, pattern);
    }

    public static SecurityEndpoint of(HttpMethod method, String pattern) {
        return new SecurityEndpoint(method, pattern);
    }

    public boolean matches(String requestMethod, String requestUri) {
        return (method == null || method.matches(requestMethod))
            && PATH_MATCHER.match(pattern, requestUri);
    }
}
