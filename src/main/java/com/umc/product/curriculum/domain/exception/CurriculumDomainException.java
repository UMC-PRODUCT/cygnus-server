package com.umc.product.curriculum.domain.exception;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.global.exception.constant.Domain;

public class CurriculumDomainException extends BusinessException {
    public CurriculumDomainException(CurriculumErrorCode errorCode) {
        super(Domain.CURRICULUM, errorCode);
    }

    public CurriculumDomainException(CurriculumErrorCode errorCode, String message) {
        super(Domain.CURRICULUM, errorCode, message);
    }

    public CurriculumDomainException(CurriculumErrorCode errorCode, Throwable cause) {
        super(Domain.CURRICULUM, errorCode, cause);
    }
}
