package com.umc.product.inquiry.application.port.in.command;

import com.umc.product.inquiry.application.port.in.command.dto.CloseInquiryCommand;

public interface CloseInquiryUseCase {

    void close(CloseInquiryCommand command);
}
