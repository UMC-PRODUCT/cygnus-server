package com.umc.product.inquiry.application.service.query;

import com.umc.product.chat.application.port.in.query.GetChatMessagesUseCase;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageCursorResult;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessagesQuery;
import com.umc.product.global.response.CursorResponse;
import com.umc.product.inquiry.application.access.InquiryAccessScope;
import com.umc.product.inquiry.application.access.InquiryAccessScopeResolver;
import com.umc.product.inquiry.application.port.in.query.GetInquiryListUseCase;
import com.umc.product.inquiry.application.port.in.query.GetInquiryMessagesUseCase;
import com.umc.product.inquiry.application.port.in.query.GetInquiryUseCase;
import com.umc.product.inquiry.application.port.in.query.dto.GetInquiryListQuery;
import com.umc.product.inquiry.application.port.in.query.dto.GetInquiryMessagesQuery;
import com.umc.product.inquiry.application.port.in.query.dto.GetInquiryQuery;
import com.umc.product.inquiry.application.port.in.query.dto.InquiryInfo;
import com.umc.product.inquiry.application.port.in.query.dto.InquirySummaryInfo;
import com.umc.product.inquiry.application.port.out.LoadInquiryPort;
import com.umc.product.inquiry.application.port.out.LoadOperatorStatusPort;
import com.umc.product.inquiry.application.port.out.dto.LoadOperatorStatusContext;
import com.umc.product.inquiry.domain.Inquiry;
import com.umc.product.inquiry.domain.exception.InquiryDomainException;
import com.umc.product.inquiry.domain.exception.InquiryErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryQueryService implements GetInquiryListUseCase, GetInquiryUseCase, GetInquiryMessagesUseCase {

    private final InquiryAccessScopeResolver scopeResolver;
    private final LoadInquiryPort loadInquiryPort;
    private final LoadOperatorStatusPort loadOperatorStatusPort;
    private final GetChatMessagesUseCase getChatMessagesUseCase;

    @Override
    public CursorResponse<InquirySummaryInfo> getList(GetInquiryListQuery query) {
        InquiryAccessScope scope = scopeResolver.resolve(query.memberId());
        List<Inquiry> rows = loadInquiryPort.listByScope(scope, query);
        return CursorResponse.of(rows, query.size(), Inquiry::getId, InquirySummaryInfo::from);
    }

    @Override
    public InquiryInfo getById(GetInquiryQuery query) {
        Inquiry inquiry = loadInquiryPort.getById(query.inquiryId());
        verifyAccess(query.requesterMemberId(), inquiry);
        return InquiryInfo.from(inquiry);
    }

    @Override
    public ChatMessageCursorResult getMessages(GetInquiryMessagesQuery query) {
        Inquiry inquiry = loadInquiryPort.getById(query.inquiryId());
        verifyAccess(query.requesterMemberId(), inquiry);

        return getChatMessagesUseCase.getMessages(new GetChatMessagesQuery(
            inquiry.getChatRoomId(),
            query.requesterMemberId(),
            query.cursorId(),
            query.size()
        ));
    }

    private void verifyAccess(Long memberId, Inquiry inquiry) {
        if (inquiry.getAuthorMemberId().equals(memberId)) {
            return;
        }
        boolean isOperator = loadOperatorStatusPort.isOperator(
            LoadOperatorStatusContext.of(memberId, inquiry));
        if (!isOperator) {
            throw new InquiryDomainException(InquiryErrorCode.NO_INQUIRY_PERMISSION);
        }
    }
}
