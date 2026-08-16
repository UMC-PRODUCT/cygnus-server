package com.umc.product.authentication.adapter.in.web;

import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.umc.product.authentication.config.SsoProperties;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SsoCookieWriter {

    private static final String COOKIE_PATH = "/";

    private final SsoProperties properties;

    public void writeLoginCookie(HttpServletResponse response, String loginToken, Instant expiresAt) {
        ResponseCookie.ResponseCookieBuilder builder = baseCookieBuilder(loginToken)
            .maxAge(maxAgeUntil(expiresAt));

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    public void clearLoginCookie(HttpServletResponse response) {
        ResponseCookie.ResponseCookieBuilder builder = baseCookieBuilder("")
            .maxAge(Duration.ZERO);

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private ResponseCookie.ResponseCookieBuilder baseCookieBuilder(String value) {
        SsoProperties.Cookie cookie = properties.cookie();
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookie.name(), value)
            .httpOnly(true)
            .secure(cookie.secure())
            .path(COOKIE_PATH);

        if (StringUtils.hasText(cookie.sameSite())) {
            builder.sameSite(cookie.sameSite());
        }
        if (StringUtils.hasText(cookie.domain())) {
            builder.domain(cookie.domain());
        }
        return builder;
    }

    private Duration maxAgeUntil(Instant expiresAt) {
        Duration maxAge = Duration.between(Instant.now(), expiresAt);
        if (maxAge.isNegative() || maxAge.isZero()) {
            return Duration.ZERO;
        }
        return maxAge;
    }
}
