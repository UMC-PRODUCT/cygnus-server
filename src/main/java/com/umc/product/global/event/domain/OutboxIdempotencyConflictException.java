package com.umc.product.global.event.domain;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.global.exception.constant.Domain;

public class OutboxIdempotencyConflictException extends BusinessException {

    public OutboxIdempotencyConflictException() {
        super(Domain.COMMON, EventOutboxErrorCode.IDEMPOTENCY_CONFLICT);
    }
}
