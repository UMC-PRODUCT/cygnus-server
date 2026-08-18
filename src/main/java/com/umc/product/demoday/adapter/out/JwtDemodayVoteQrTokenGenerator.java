package com.umc.product.demoday.adapter.out;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.out.GenerateDemodayVoteQrCredentialPort;
import com.umc.product.demoday.config.DemodayVoteQrProperties;
import com.umc.product.demoday.domain.enums.DemodayVoteQrPurpose;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtDemodayVoteQrTokenGenerator implements GenerateDemodayVoteQrCredentialPort {

    private static final String PURPOSE_CLAIM = "purpose";
    private static final String POLL_ID_CLAIM = "pollId";

    private final SecretKey signingKey;

    public JwtDemodayVoteQrTokenGenerator(DemodayVoteQrProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(properties.signingKey().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 같은 pollId·같은 구간이면 항상 같은 서명 결과를 반환한다. HMAC 서명은 입력(header+claims)과 키가
     * 같으면 결정적이므로, 이 메서드가 매 호출마다 같은 claim 값만 넣으면 재발급 여부와 무관하게
     * 동일한 문자열이 나온다. jti 같은 무작위 claim은 추가하지 않는다.
     */
    @Override
    public String generate(Long pollId, Instant issuedAt, Instant expiresAt) {
        return Jwts.builder()
            .claim(PURPOSE_CLAIM, DemodayVoteQrPurpose.VOTE_AUTH.name())
            .claim(POLL_ID_CLAIM, pollId)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .signWith(signingKey)
            .compact();
    }
}
