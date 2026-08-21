package com.umc.product.demoday.adapter.in.web.security;

import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 게스트 participant token Cookie를 응답에 싣는다. Domain 속성은 의도적으로 지정하지 않는다(host-only)
 * 이 Cookie를 읽는 요청은 항상 이 API를 발급한 호스트로만 가므로 Domain을 넓히면 다른 서브도메인까지
 * Cookie를 읽게 만들어 공격 표면만 늘어나기 때문이다.
 */
@Component
public class DemodayParticipantCookieWriter {

    // 데모데이에서만 사용하는 쿠키 경로이기 때문에 "/api/v1/demoday"로 변경 필요 있음.
    // 단, 최종 happy-test 전까지는 "/" 경로로 두고 FE와 연결을 진행할 예정.
    private static final String COOKIE_PATH = "/";

    public void writeParticipantCookie(HttpServletResponse response, String token, Instant expiresAt) {
        ResponseCookie cookie = ResponseCookie.from(DemodayParticipantTokenProvider.COOKIE_NAME, token)
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path(COOKIE_PATH)
            .maxAge(maxAgeUntil(expiresAt))
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private Duration maxAgeUntil(Instant expiresAt) {
        Duration maxAge = Duration.between(Instant.now(), expiresAt);
        return maxAge.isNegative() || maxAge.isZero() ? Duration.ZERO : maxAge;
    }
}
