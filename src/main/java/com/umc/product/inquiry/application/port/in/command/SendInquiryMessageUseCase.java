package com.umc.product.inquiry.application.port.in.command;

import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.inquiry.application.port.in.command.dto.SendInquiryMessageCommand;

public interface SendInquiryMessageUseCase {

    ChatMessageInfo send(SendInquiryMessageCommand command);
}
