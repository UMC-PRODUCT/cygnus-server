package com.umc.product.inquiry.application.port.out;

import com.umc.product.inquiry.domain.Inquiry;

public interface SaveInquiryPort {

    Inquiry save(Inquiry inquiry);
}
