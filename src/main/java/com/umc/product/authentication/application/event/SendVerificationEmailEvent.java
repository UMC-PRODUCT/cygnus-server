package com.umc.product.authentication.application.event;

import java.time.Instant;
import java.util.UUID;

import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.event.domain.OutboxDispatchMode;

/**
 * 이메일 인증 세션 생성/재발급 후, 메일 발송이 필요할 때 트랜잭션 commit 직후
 * 비동기로 메일을 보내기 위해 발행되는 도메인 이벤트.
 * <p>
 * SMTP 호출이 트랜잭션 내부에서 일어나면 메일 서버 응답이 느릴 때 DB 커넥션을 길게
 * 점유하고, 발송 실패가 세션 롤백을 유발해 사용자가 처음부터 다시 인증해야 하는
 * 문제가 있었다. AFTER_COMMIT 단계로 분리해 양쪽 부작용을 끊는다.
 * <p>
 * {@code eventId}와 {@code occurredAt}을 명시하지 않으면 각각 {@link UUID#randomUUID()}와
 * {@link Instant#now()}로 자동 채워진다. 응용 레이어에서는 {@link #of(String, String)}
 * 정적 팩토리를 사용한다.
 */
public record SendVerificationEmailEvent(
    UUID eventId,
    Instant occurredAt,
    String email,
    String verificationCode
) implements DomainEvent {

    public SendVerificationEmailEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public static SendVerificationEmailEvent of(String email, String verificationCode) {
        return new SendVerificationEmailEvent(null, null, email, verificationCode);
    }

    @Override
    public String eventType() {
        return "authentication.email.verification.requested";
    }

    @Override
    public OutboxDispatchMode outboxDispatchMode() {
        return OutboxDispatchMode.NON_TRANSACTIONAL;
    }
}
