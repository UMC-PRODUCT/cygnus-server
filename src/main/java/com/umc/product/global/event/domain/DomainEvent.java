package com.umc.product.global.event.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * 모든 도메인 이벤트가 구현해야 하는 마커 인터페이스.
 * <p>
 * 메타데이터는 다음 용도를 가진다.
 * <ul>
 *     <li>{@code eventId} — 멱등성 처리를 위한 고유 식별자.</li>
 *     <li>{@code occurredAt} — 이벤트가 발생한 시각.</li>
 *     <li>{@code eventType} — 이벤트 종류를 식별하는 문자열 (예: "audit.log.register").
 *         향후 브로커 도입 시 topic/queue 라우팅 키로 사용된다.</li>
 * </ul>
 */
public interface DomainEvent {

    UUID eventId();

    Instant occurredAt();

    String eventType();

    default OutboxDispatchMode outboxDispatchMode() {
        return OutboxDispatchMode.TRANSACTIONAL;
    }
}
