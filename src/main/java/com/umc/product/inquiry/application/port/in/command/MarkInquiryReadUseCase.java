package com.umc.product.inquiry.application.port.in.command;

import com.umc.product.inquiry.application.port.in.command.dto.MarkInquiryReadCommand;

public interface MarkInquiryReadUseCase {

    void markRead(MarkInquiryReadCommand command);
}
