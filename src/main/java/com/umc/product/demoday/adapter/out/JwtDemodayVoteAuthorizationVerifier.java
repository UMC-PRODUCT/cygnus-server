package com.umc.product.demoday.adapter.out;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantType;
import com.umc.product.demoday.application.port.out.DemodayVoteAuthorizationTokenClaims;
import com.umc.product.demoday.application.port.out.VerifyDemodayVoteAuthorizationPort;
import com.umc.product.demoday.config.DemodayVoteAuthorizationProperties;
import com.umc.product.demoday.config.DemodayVoteQrProperties;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtDemodayVoteAuthorizationVerifier implements VerifyDemodayVoteAuthorizationPort {

    private static final String PURPOSE_CLAIM = "purpose";
    private static final String PARTICIPANT_TYPE_CLAIM = "participantType";
    private static final String PARTICIPANT_ID_CLAIM = "participantId";
    private static final String POLL_ID_CLAIM = "pollId";
    private static final String BOOTH_ID_CLAIM = "boothId";

    private final SecretKey signingKey;
    private final Clock clock;

    public JwtDemodayVoteAuthorizationVerifier(
        DemodayVoteAuthorizationProperties properties,
        DemodayVoteQrProperties voteQrProperties,
        Clock clock
    ) {
        if (properties.signingKey().equals(voteQrProperties.signingKey())) {
            throw new IllegalArgumentException(
                "demoday.vote-authorization.signing-key must be different from demoday.vote-qr.signing-key"
            );
        }
        this.signingKey = Keys.hmacShaKeyFor(properties.signingKey().getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    @Override
    public DemodayVoteAuthorizationTokenClaims verify(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();

            DemodayVoteAuthorizationTokenClaims tokenClaims = new DemodayVoteAuthorizationTokenClaims(
                claims.get(PURPOSE_CLAIM, String.class),
                toParticipantType(claims),
                toLong(claims, PARTICIPANT_ID_CLAIM),
                toLong(claims, POLL_ID_CLAIM),
                toLong(claims, BOOTH_ID_CLAIM),
                toInstant(claims.getIssuedAt()),
                toInstant(claims.getExpiration())
            );

            if (!clock.instant().isBefore(tokenClaims.expiresAt())) {
                throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_AUTHORIZATION_EXPIRED);
            }
            return tokenClaims;
        } catch (ExpiredJwtException exception) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_AUTHORIZATION_EXPIRED);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_AUTHORIZATION_INVALID);
        }
    }

    private DemodayParticipantType toParticipantType(Claims claims) {
        String participantType = claims.get(PARTICIPANT_TYPE_CLAIM, String.class);
        if (participantType == null) {
            throw new IllegalArgumentException("participantType claim is required");
        }
        return DemodayParticipantType.valueOf(participantType);
    }

    private Long toLong(Claims claims, String claimName) {
        Number value = claims.get(claimName, Number.class);
        if (value == null) {
            throw new IllegalArgumentException(claimName + " claim is required");
        }
        return value.longValue();
    }

    private Instant toInstant(Date value) {
        if (value == null) {
            throw new IllegalArgumentException("time claim is required");
        }
        return value.toInstant();
    }
}
