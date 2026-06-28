package com.umc.product.inquiry.application.port.in.command;

import com.umc.product.inquiry.application.port.in.command.dto.TransferInquiryManagerCommand;

public interface TransferInquiryManagerUseCase {

    void transfer(TransferInquiryManagerCommand command);
}
