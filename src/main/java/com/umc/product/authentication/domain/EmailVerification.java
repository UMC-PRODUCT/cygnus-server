package com.umc.product.authentication.domain;

import java.time.Duration;
import java.time.Instant;

import com.umc.product.authentication.domain.exception.AuthenticationDomainException;
import com.umc.product.authentication.domain.exception.AuthenticationErrorCode;
import com.umc.product.authentication.domain.exception.EmailVerificationThrottledException;
import com.umc.product.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "email_verification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseEntity {

    /**
     * 인증 세션의 유효 기간 (초). 발급 / 재발급 시 expires_at = now + 이 값.
     */
    public static final long SESSION_VALIDITY_SECONDS = 10 * 60;

    /**
     * 인증 코드 brute-force 방어를 위한 최대 시도 횟수. 초과 시 세션을 즉시 무효화한다.
     */
    public static final int MAX_ATTEMPT_COUNT = 5;

    /**
     * 같은 이메일 / 같은 세션에 대한 연속 발송 간 최소 간격(초). 메일 폭주 방어.
     */
    public static final long MIN_SEND_INTERVAL_SECONDS = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 200)
    private String email;

    /**
     * UUID, 인증 토큰임
     */
    @Column(name = "token", nullable = false, length = 100)
    private String token;

    /**
     * 6자리 숫자, 인증 코드
     */
    @Column(name = "code", nullable = false, length = 10)
    private String code;


    @Column(name = "is_verified", nullable = false)
    private boolean isVerified;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verified_by", length = 30)
    private String verifiedBy;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * 인증 세션의 용도. cross-purpose 공격 방어를 위해 세션 단위로 고정한다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private EmailVerificationPurpose purpose;

    /**
     * 인증 코드 검증 시도 횟수. MAX_ATTEMPT_COUNT 도달 시 즉시 만료(세션 무효화)한다.
     * regenerate 시 0 으로 초기화한다.
     */
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    /**
     * 마지막 메일 발송 요청을 접수한 시각. 실제 발송 성공 여부와 관계없이 throttle 검사에 사용한다.
     * silent skip (예: PASSWORD_RESET 미가입) 인 경우에는 갱신하지 않는다.
     */
    @Column(name = "last_sent_at")
    private Instant lastSentAt;


    @Builder
    public EmailVerification(String email, String token, String code, EmailVerificationPurpose purpose) {
        this.email = email;
        this.token = token;
        this.code = code;
        this.purpose = purpose;
        this.expiresAt = Instant.now().plusSeconds(SESSION_VALIDITY_SECONDS); // 10분 후 만료
        this.isVerified = false;
        this.attemptCount = 0;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    /**
     * 마지막 발송 요청 접수 시각으로부터 MIN_SEND_INTERVAL_SECONDS 가 지나지 않았다면 throttle 위반.
     * lastSentAt 이 null 이면 즉시 발송 요청을 접수할 수 있다.
     */
    public boolean isSendThrottled() {
        return remainingSendIntervalSeconds() > 0;
    }

    public void validateSendInterval() {
        long remainingSeconds = remainingSendIntervalSeconds();
        if (remainingSeconds > 0) {
            throw new EmailVerificationThrottledException(remainingSeconds);
        }
    }

    private long remainingSendIntervalSeconds() {
        if (this.lastSentAt == null) {
            return 0;
        }
        Duration remaining = Duration.between(
            Instant.now(), this.lastSentAt.plusSeconds(MIN_SEND_INTERVAL_SECONDS));
        if (remaining.isNegative() || remaining.isZero()) {
            return 0;
        }
        // 초 단위 응답을 내림하면 제한이 풀리기 전에 다시 요청하게 되므로 올림한다.
        return remaining.getSeconds() + (remaining.getNano() > 0 ? 1 : 0);
    }

    /**
     * 발송 요청 접수 시점을 기록한다. 거절된 요청과 비동기 발송 결과는 이 시각을 변경하지 않는다.
     */
    public void markSent() {
        this.lastSentAt = Instant.now();
    }

    /**
     * 인증 코드 검증.
     * <p>
     * 사용자 열거 / 코드 탐색 방어를 위해 다음 실패 케이스는 외부에 모두 동일한
     * INVALID_EMAIL_VERIFICATION 응답으로 수렴시킨다:
     * <ul>
     *   <li>이미 검증 완료된 세션의 재시도 (verifiedAt / verifiedBy 덮어쓰기 방지 가드)</li>
     *   <li>임계치 초과로 무효화된 세션의 추가 시도 (attempt_count 더 이상 증가시키지 않음)</li>
     *   <li>만료된 세션</li>
     *   <li>코드 불일치</li>
     * </ul>
     * 운영 디버깅에서 상태 구분이 필요하면 attemptCount / isVerified / expiresAt /
     * lastSentAt 으로 사후 분석한다.
     */
    public void verifyCode(String code) {
        // 1. 이미 검증 완료된 세션: 부수효과 없이 즉시 거부 (verifiedAt 덮어쓰기 방지)
        if (this.isVerified) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION);
        }

        // 2. 임계치 초과로 무효화된 세션: 시도 횟수도 더 이상 증가시키지 않고 즉시 거부
        if (this.attemptCount >= MAX_ATTEMPT_COUNT) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION);
        }

        this.attemptCount++;

        if (this.code.equals(code) && !isExpired()) {
            setVerified("CODE");
            return;
        }

        // 이번 시도로 임계치에 도달했다면 즉시 세션 만료시켜 후속 시도를 차단한다.
        if (this.attemptCount >= MAX_ATTEMPT_COUNT) {
            this.expiresAt = Instant.now();
        }

        throw new AuthenticationDomainException(AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION);
    }

    public void regenerate(String newCode, String newToken) {
        if (this.isVerified) {
            throw new AuthenticationDomainException(AuthenticationErrorCode.ALREADY_VERIFIED_EMAIL);
        }

        this.code = newCode;
        this.token = newToken;
        this.expiresAt = Instant.now().plusSeconds(SESSION_VALIDITY_SECONDS);
        this.attemptCount = 0;
    }

    private void setVerified(String method) {
        this.isVerified = true;
        this.verifiedAt = Instant.now();
        this.verifiedBy = method;
    }
}
