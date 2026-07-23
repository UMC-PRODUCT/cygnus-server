package com.umc.product.notification.domain.exception;

import com.umc.product.global.event.domain.OutboxDispatchFailure;
import com.umc.product.global.exception.BusinessException;
import com.umc.product.global.exception.constant.Domain;

public class EmailDomainException extends BusinessException implements OutboxDispatchFailure {

    private final boolean retryable;

    public EmailDomainException(EmailErrorCode emailErrorCode) {
        this(emailErrorCode, true);
    }

    public EmailDomainException(EmailErrorCode emailErrorCode, boolean retryable) {
        super(Domain.EMAIL, emailErrorCode);
        this.retryable = retryable;
    }

    public EmailDomainException(EmailErrorCode emailErrorCode, Throwable cause) {
        this(emailErrorCode, true, cause);
    }

    public EmailDomainException(EmailErrorCode emailErrorCode, boolean retryable, Throwable cause) {
        super(Domain.EMAIL, emailErrorCode, cause);
        this.retryable = retryable;
    }

    @Override
    public boolean retryable() {
        return retryable;
    }
}
