package com.umc.product.demoday.adapter.out;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;
import com.umc.product.demoday.application.port.out.GenerateDemodayVoteAuthorizationPort;
import com.umc.product.demoday.config.DemodayVoteAuthorizationProperties;
import com.umc.product.demoday.domain.enums.DemodayVoteAuthorizationPurpose;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtDemodayVoteAuthorizationGenerator implements GenerateDemodayVoteAuthorizationPort {

    private static final String PURPOSE_CLAIM = "purpose";
    private static final String PARTICIPANT_TYPE_CLAIM = "participantType";
    private static final String PARTICIPANT_ID_CLAIM = "participantId";
    private static final String POLL_ID_CLAIM = "pollId";
    private static final String BOOTH_ID_CLAIM = "boothId";

    private final SecretKey signingKey;

    public JwtDemodayVoteAuthorizationGenerator(DemodayVoteAuthorizationProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(properties.signingKey().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generate(
        DemodayParticipant participant,
        Long pollId,
        Long boothId,
        Instant issuedAt,
        Instant expiresAt
    ) {
        return Jwts.builder()
            .claim(PURPOSE_CLAIM, DemodayVoteAuthorizationPurpose.CAST_VOTE.name())
            .claim(PARTICIPANT_TYPE_CLAIM, participant.participantType().name())
            .claim(PARTICIPANT_ID_CLAIM, participant.participantId())
            .claim(POLL_ID_CLAIM, pollId)
            .claim(BOOTH_ID_CLAIM, boothId)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .signWith(signingKey)
            .compact();
    }
}
