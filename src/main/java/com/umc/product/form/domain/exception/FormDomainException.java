package com.umc.product.form.domain.exception;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.global.exception.constant.Domain;

public class FormDomainException extends BusinessException {
    public FormDomainException(FormErrorCode formErrorCode) {
        super(Domain.FORM, formErrorCode);
    }

    public FormDomainException(FormErrorCode formErrorCode, String message) {
        super(Domain.FORM, formErrorCode, message);
    }
}
