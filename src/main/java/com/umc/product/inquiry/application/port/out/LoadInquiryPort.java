package com.umc.product.inquiry.application.port.out;

import com.umc.product.inquiry.application.access.InquiryAccessScope;
import com.umc.product.inquiry.application.port.in.query.dto.GetInquiryListQuery;
import com.umc.product.inquiry.domain.Inquiry;
import java.util.List;

public interface LoadInquiryPort {

    Inquiry getById(Long inquiryId);

    Inquiry getByChatRoomId(Long chatRoomId);

    List<Inquiry> listByScope(InquiryAccessScope scope, GetInquiryListQuery filter);
}
