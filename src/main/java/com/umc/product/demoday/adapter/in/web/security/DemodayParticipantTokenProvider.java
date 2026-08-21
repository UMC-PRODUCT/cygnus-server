package com.umc.product.demoday.adapter.in.web.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.out.IssueDemodayParticipantTokenPort;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * 게스트 참여(participant) 토큰의 발급·검증을 전담한다.
 * <p>
 * 회원 {@code JwtTokenProvider}와는 전용 secret으로 완전히 분리된다.
 * 기존 인증 코어를 건드리지 않고 게스트 인증을 완전히 별도 경로로 얹기 위함이다.
 * 세션을 DB에 두지 않고 subject(entryCodeId)만 서명해 무상태로 유지한다.
 * 참여 진행 상태는 저장하지 않고 스탬프·투표 기록에서 매번 파생하므로,
 * 이 토큰 자체도 저장된 세션이 아니라 "이 entryCodeId로 입장했다"는 자기서명 증명일 뿐이다.
 * 만료 시각은 고정 TTL이 아니라 발급 시점의 poll.closesAt을 그대로 받는다(계약: "Cookie와 입장 코드의
 * 수명은 Poll 종료 시점까지").
 */
@Slf4j
@Component
public class DemodayParticipantTokenProvider implements IssueDemodayParticipantTokenPort {

    public static final String COOKIE_NAME = "demoday_participant_token";

    private final SecretKey secret;

    public DemodayParticipantTokenProvider(
        @Value("${demoday.participant-token.secret}") String secret
    ) {
        this.secret = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String issue(Long entryCodeId, Instant expiresAt) {
        return Jwts.builder()
            .subject(String.valueOf(entryCodeId))
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(expiresAt))
            .signWith(secret)
            .compact();
    }

    /**
     * 서명·형식·만료 중 무엇이 틀려도 예외를 던지지 않고 빈 값을 반환한다.
     * 필터에서 실패를 인증 거부가 아니라 "이 요청엔 유효한 게스트 세션이 없다"로만 취급하기 위함이다
     * ({@code JwtAuthenticationFilter}와 동일한 fail-open 원칙).
     */
    public Optional<Long> parseEntryCodeId(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secret)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            return Optional.of(Long.parseLong(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("게스트 participant token 파싱 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
