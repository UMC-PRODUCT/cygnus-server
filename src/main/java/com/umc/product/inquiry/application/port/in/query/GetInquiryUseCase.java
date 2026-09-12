package com.umc.product.inquiry.application.port.in.query;

import com.umc.product.inquiry.application.port.in.query.dto.GetInquiryQuery;
import com.umc.product.inquiry.application.port.in.query.dto.InquiryInfo;

public interface GetInquiryUseCase {

    InquiryInfo getById(GetInquiryQuery query);
}
