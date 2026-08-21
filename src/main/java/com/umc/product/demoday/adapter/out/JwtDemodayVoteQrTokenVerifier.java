package com.umc.product.demoday.adapter.out;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.out.DemodayVoteQrTokenClaims;
import com.umc.product.demoday.application.port.out.VerifyDemodayVoteQrCredentialPort;
import com.umc.product.demoday.config.DemodayVoteQrProperties;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtDemodayVoteQrTokenVerifier implements VerifyDemodayVoteQrCredentialPort {

    private static final String PURPOSE_CLAIM = "purpose";
    private static final String POLL_ID_CLAIM = "pollId";

    /**
     * 시간 구간 경계 직전에 발급된 token이 구간 교체 직후 요청에서 부당하게 거부되지 않도록, 검증 시점에만 허용하는 유예 폭이다.
     * 발급 쪽 claim(iat/exp)에는 영향을 주지 않으므로 조회 응답의 expiresAt은 항상 정시로 유지된다.
     */
    private static final long ALLOWED_CLOCK_SKEW_SECONDS = 60;

    private final SecretKey signingKey;
    private final Clock clock;

    public JwtDemodayVoteQrTokenVerifier(DemodayVoteQrProperties properties, Clock clock) {
        this.signingKey = Keys.hmacShaKeyFor(properties.signingKey().getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    @Override
    public DemodayVoteQrTokenClaims verify(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .clockSkewSeconds(ALLOWED_CLOCK_SKEW_SECONDS)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();

            return new DemodayVoteQrTokenClaims(
                claims.get(PURPOSE_CLAIM, String.class),
                toPollId(claims),
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant()
            );
        } catch (ExpiredJwtException exception) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_QR_EXPIRED);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_QR_INVALID);
        }
    }

    private Long toPollId(Claims claims) {
        //Ineger와 Long 상관없이 받을 수 있도록 하는 방어코드
        Number pollId = claims.get(POLL_ID_CLAIM, Number.class);
        if (pollId == null) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_QR_INVALID);
        }
        return pollId.longValue();
    }
}
