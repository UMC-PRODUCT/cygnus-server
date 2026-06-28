package com.umc.product.inquiry.application.port.in.command;

import com.umc.product.inquiry.application.port.in.command.dto.AssignInquiryManagerCommand;

public interface AssignInquiryManagerUseCase {

    void assign(AssignInquiryManagerCommand command);
}
