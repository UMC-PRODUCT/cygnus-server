package com.umc.product.global.event.domain;

/**
 * Outbox listener 실패가 재시도 가능한지 relay에 전달하는 공용 계약.
 */
public interface OutboxDispatchFailure {

    boolean retryable();
}
