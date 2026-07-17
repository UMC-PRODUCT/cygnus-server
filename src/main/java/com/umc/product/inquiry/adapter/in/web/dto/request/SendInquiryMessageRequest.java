package com.umc.product.inquiry.adapter.in.web.dto.request;

import java.util.List;

import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.inquiry.application.port.in.command.dto.SendInquiryMessageCommand;

import jakarta.validation.constraints.NotNull;

public record SendInquiryMessageRequest(
    @NotNull(message = "콘텐츠 타입은 필수입니다.") MessageContentType contentType,
    String content,
    List<String> fileMetadataIds
) {
    public SendInquiryMessageCommand toCommand(Long inquiryId, Long senderMemberId) {
        return new SendInquiryMessageCommand(inquiryId, senderMemberId, contentType, content, fileMetadataIds);
    }
}
