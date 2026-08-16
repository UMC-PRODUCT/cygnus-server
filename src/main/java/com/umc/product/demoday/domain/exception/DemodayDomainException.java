package com.umc.product.demoday.domain.exception;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.global.exception.constant.Domain;

public class DemodayDomainException extends BusinessException {

    public DemodayDomainException(DemodayErrorCode demodayErrorCode) {
        super(Domain.DEMODAY, demodayErrorCode);
    }

    public DemodayDomainException(DemodayErrorCode demodayErrorCode, String message) {
        super(Domain.DEMODAY, demodayErrorCode, message);
    }

    public DemodayDomainException(DemodayErrorCode demodayErrorCode, Throwable cause) {
        super(Domain.DEMODAY, demodayErrorCode, cause);
    }
}
