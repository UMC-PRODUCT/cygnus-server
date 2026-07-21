package com.umc.product.recruiting.domain.exception;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.global.exception.constant.Domain;

public class RecruitingDomainException extends BusinessException {

    public RecruitingDomainException(RecruitingErrorCode recruitingErrorCode) {
        super(Domain.RECRUITMENT, recruitingErrorCode);
    }

    public RecruitingDomainException(RecruitingErrorCode recruitingErrorCode, String message) {
        super(Domain.RECRUITMENT, recruitingErrorCode, message);
    }
}
