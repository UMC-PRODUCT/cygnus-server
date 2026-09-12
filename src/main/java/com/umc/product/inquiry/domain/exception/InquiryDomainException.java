package com.umc.product.inquiry.domain.exception;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.global.exception.constant.Domain;

public class InquiryDomainException extends BusinessException {

    public InquiryDomainException(InquiryErrorCode inquiryErrorCode) {
        super(Domain.INQUIRY, inquiryErrorCode);
    }

    public InquiryDomainException(InquiryErrorCode inquiryErrorCode, String message) {
        super(Domain.INQUIRY, inquiryErrorCode, message);
    }
}
