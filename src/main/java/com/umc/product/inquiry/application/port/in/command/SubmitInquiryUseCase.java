package com.umc.product.inquiry.application.port.in.command;

import com.umc.product.inquiry.application.port.in.command.dto.SubmitInquiryCommand;
import com.umc.product.inquiry.application.port.in.query.dto.InquiryInfo;

public interface SubmitInquiryUseCase {

    InquiryInfo submit(SubmitInquiryCommand command);
}
