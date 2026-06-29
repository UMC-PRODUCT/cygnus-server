package com.umc.product.inquiry.application.port.in.query;

import com.umc.product.chat.application.port.in.query.dto.ChatMessageCursorResult;
import com.umc.product.inquiry.application.port.in.query.dto.GetInquiryMessagesQuery;

public interface GetInquiryMessagesUseCase {

    ChatMessageCursorResult getMessages(GetInquiryMessagesQuery query);
}
