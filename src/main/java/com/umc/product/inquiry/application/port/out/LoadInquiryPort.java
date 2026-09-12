package com.umc.product.inquiry.application.port.out;

import java.util.List;

import com.umc.product.inquiry.application.access.InquiryAccessScope;
import com.umc.product.inquiry.application.port.in.query.dto.GetInquiryListQuery;
import com.umc.product.inquiry.domain.Inquiry;

public interface LoadInquiryPort {

    Inquiry getById(Long inquiryId);

    /**
     * chatRoomId로 문의를 조회한다.
     * <p>
     * broadcast 리스너가 {@code ChatMessageCreatedEvent.roomId()}를 inquiryId로 역매핑할 때 사용한다.
     * 해당 채팅방과 연결된 문의가 없으면 {@link com.umc.product.inquiry.domain.exception.InquiryDomainException}을 던진다.
     *
     * @param chatRoomId chat 도메인의 채팅방 ID
     * @return 해당 채팅방에 연결된 Inquiry
     */
    Inquiry getByRoomId(Long chatRoomId);

    List<Inquiry> listByScope(InquiryAccessScope scope, GetInquiryListQuery filter);
}
