package com.umc.product.authentication.adapter.in.web.dto.request;

import org.springframework.util.StringUtils;

import com.umc.product.common.domain.enums.ClientType;

public record KakaoLoginRequest(
    // OIDC 방식 로그인에 사용할 Kakao ID Token.
    String idToken,

    // 기존 클라이언트 호환용 legacy Access Token. 신규 클라이언트는 idToken을 사용한다.
    String accessToken,

    // 클라이언트 플랫폼(ANDROID/IOS/WEB). 트래픽 분포 분석을 위해 AT claim 으로 흘려보낸다.
    // 도입 이전 클라이언트 호환을 위해 nullable. 누락 시 AT claim 자체가 생략되며
    // 다운스트림 (MDC / 통계) 에서는 "UNKNOWN" 으로 집계된다.
    ClientType clientType
) {
    public KakaoLoginRequest {
        if (!StringUtils.hasText(idToken) && !StringUtils.hasText(accessToken)) {
            throw new IllegalArgumentException("idToken 또는 accessToken 중 하나는 필수입니다.");
        }
    }

    public String token() {
        if (StringUtils.hasText(idToken)) {
            return idToken;
        }
        return accessToken;
    }
}
