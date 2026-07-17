package com.umc.product.inquiry.application.port.out;

import java.util.List;

import com.umc.product.inquiry.application.access.InquiryAccessScope;
import com.umc.product.inquiry.application.port.in.query.dto.GetInquiryListQuery;
import com.umc.product.inquiry.domain.Inquiry;

public interface LoadInquiryPort {

    Inquiry getById(Long inquiryId);

    List<Inquiry> listByScope(InquiryAccessScope scope, GetInquiryListQuery filter);
}
