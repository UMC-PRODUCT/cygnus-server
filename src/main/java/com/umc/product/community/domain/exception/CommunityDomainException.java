package com.umc.product.community.domain.exception;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.global.exception.constant.Domain;

public class CommunityDomainException extends BusinessException {
    public CommunityDomainException(CommunityErrorCode errorCode) {
        super(Domain.COMMUNITY, errorCode);
    }

    public CommunityDomainException(CommunityErrorCode errorCode, String message) {
        super(Domain.COMMUNITY, errorCode, message);
    }

    public CommunityDomainException(CommunityErrorCode errorCode, Throwable cause) {
        super(Domain.COMMUNITY, errorCode, cause);
    }
}
