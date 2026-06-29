package com.umc.product.inquiry.application.service.query;

import com.umc.product.global.response.CursorResponse;
import com.umc.product.inquiry.application.access.InquiryAccessScope;
import com.umc.product.inquiry.application.access.InquiryAccessScopeResolver;
import com.umc.product.inquiry.application.port.in.query.GetInquiryListUseCase;
import com.umc.product.inquiry.application.port.in.query.dto.GetInquiryListQuery;
import com.umc.product.inquiry.application.port.in.query.dto.InquirySummaryInfo;
import com.umc.product.inquiry.application.port.out.LoadInquiryPort;
import com.umc.product.inquiry.domain.Inquiry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryQueryService implements GetInquiryListUseCase {

    private final InquiryAccessScopeResolver scopeResolver;
    private final LoadInquiryPort loadInquiryPort;

    @Override
    public CursorResponse<InquirySummaryInfo> getList(GetInquiryListQuery query) {
        InquiryAccessScope scope = scopeResolver.resolve(query.memberId());
        List<Inquiry> rows = loadInquiryPort.listByScope(scope, query);
        return CursorResponse.of(rows, query.size(), Inquiry::getId, InquirySummaryInfo::from);
    }
}
