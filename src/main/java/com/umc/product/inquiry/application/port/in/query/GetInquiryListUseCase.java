package com.umc.product.inquiry.application.port.in.query;

import com.umc.product.global.response.CursorResponse;
import com.umc.product.inquiry.application.port.in.query.dto.GetInquiryListQuery;
import com.umc.product.inquiry.application.port.in.query.dto.InquirySummaryInfo;

public interface GetInquiryListUseCase {

    CursorResponse<InquirySummaryInfo> getList(GetInquiryListQuery query);
}
