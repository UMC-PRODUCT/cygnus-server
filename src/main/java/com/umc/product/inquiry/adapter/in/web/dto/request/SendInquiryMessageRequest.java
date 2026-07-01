package com.umc.product.inquiry.adapter.in.web.dto.request;

import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.inquiry.application.port.in.command.dto.SendInquiryMessageCommand;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SendInquiryMessageRequest(
    @NotNull(message = "콘텐츠 타입은 필수입니다.")
    MessageContentType contentType,
    String content,
    List<String> fileMetadataIds
) {
    public SendInquiryMessageCommand toCommand(Long chatRoomId, Long senderMemberId) {
        return new SendInquiryMessageCommand(chatRoomId, senderMemberId, contentType, content, fileMetadataIds);
    }
}
