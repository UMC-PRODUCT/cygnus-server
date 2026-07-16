package com.umc.product.inquiry.application.service.query;

import com.umc.product.chat.application.port.in.query.GetChatMessagesUseCase;
import com.umc.product.chat.application.port.in.query.ListChatRoomSummariesUseCase;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageCursorResult;
import com.umc.product.chat.application.port.in.query.dto.ChatRoomSummaryInfo;
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
import java.util.Map;
import java.util.stream.Collectors;
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
    private final ListChatRoomSummariesUseCase listChatRoomSummariesUseCase;

    @Override
    public CursorResponse<InquirySummaryInfo> getList(GetInquiryListQuery query) {
        InquiryAccessScope scope = scopeResolver.resolve(query.memberId());
        List<Inquiry> rows = loadInquiryPort.listByScope(scope, query);

        List<Long> roomIds = rows.stream()
            .map(Inquiry::getChatRoomId)
            .toList();
        Map<Long, Long> unreadByRoom = buildUnreadMap(query.memberId(), roomIds);

        boolean hasNext = rows.size() > query.size();
        List<Inquiry> page = hasNext ? rows.subList(0, query.size()) : rows;

        List<InquirySummaryInfo> content = page.stream()
            .map(inquiry -> InquirySummaryInfo.from(inquiry, unreadByRoom.getOrDefault(inquiry.getChatRoomId(), 0L)))
            .toList();

        Long nextCursor = hasNext && !page.isEmpty() ? page.get(page.size() - 1).getId() : null;
        return new CursorResponse<>(content, nextCursor, hasNext);
    }

    @Override
    public InquiryInfo getById(GetInquiryQuery query) {
        Inquiry inquiry = loadInquiryPort.getById(query.inquiryId());
        verifyAccess(query.requesterMemberId(), inquiry);

        Map<Long, Long> unreadByRoom = buildUnreadMap(
            query.requesterMemberId(), List.of(inquiry.getChatRoomId()));
        long unreadCount = unreadByRoom.getOrDefault(inquiry.getChatRoomId(), 0L);
        return InquiryInfo.from(inquiry, unreadCount);
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

    /**
     * 요청자의 채팅방별 미읽음 수 맵을 구성한다. 요청자가 멤버가 아닌 방은 결과에 포함되지 않으므로 0으로 처리된다.
     */
    private Map<Long, Long> buildUnreadMap(Long memberId, List<Long> roomIds) {
        if (roomIds.isEmpty()) {
            return Map.of();
        }
        return listChatRoomSummariesUseCase.listRoomSummaries(memberId, roomIds).stream()
            .collect(Collectors.toMap(ChatRoomSummaryInfo::roomId, ChatRoomSummaryInfo::unreadCount));
    }
}
