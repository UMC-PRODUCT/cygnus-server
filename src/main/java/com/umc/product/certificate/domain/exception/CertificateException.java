package com.umc.product.certificate.domain.exception;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.global.exception.constant.Domain;

public class CertificateException extends BusinessException {

    public CertificateException(CertificateErrorCode errorCode) {
        super(Domain.CERTIFICATE, errorCode);
    }

    public CertificateException(CertificateErrorCode errorCode, Throwable cause) {
        super(Domain.CERTIFICATE, errorCode, cause);
    }
}
