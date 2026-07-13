package com.umc.product.organization.exception;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.global.exception.constant.Domain;

public class OrganizationDomainException extends BusinessException {
    public OrganizationDomainException(OrganizationErrorCode errorCode) {
        super(Domain.ORGANIZATION, errorCode);
    }

    public OrganizationDomainException(OrganizationErrorCode errorCode, String message) {
        super(Domain.ORGANIZATION, errorCode, message);
    }

    public OrganizationDomainException(OrganizationErrorCode errorCode, Throwable cause) {
        super(Domain.ORGANIZATION, errorCode, cause);
    }
}
