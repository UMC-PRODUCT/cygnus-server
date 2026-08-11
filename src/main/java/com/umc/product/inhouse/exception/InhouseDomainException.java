package com.umc.product.inhouse.exception;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.global.exception.constant.Domain;

public class InhouseDomainException extends BusinessException {

    public InhouseDomainException(InhouseErrorCode errorCode) {
        super(Domain.INHOUSE, errorCode);
    }

    public InhouseDomainException(InhouseErrorCode errorCode, String message) {
        super(Domain.INHOUSE, errorCode, message);
    }

    public InhouseDomainException(InhouseErrorCode errorCode, Throwable cause) {
        super(Domain.INHOUSE, errorCode, cause);
    }
}
