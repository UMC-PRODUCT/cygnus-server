package com.umc.product.authentication.domain.exception;

import lombok.Getter;

@Getter
public class EmailVerificationThrottledException extends AuthenticationDomainException {

    private final long retryAfterSeconds;

    public EmailVerificationThrottledException(long retryAfterSeconds) {
        super(AuthenticationErrorCode.EMAIL_VERIFICATION_THROTTLED);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
