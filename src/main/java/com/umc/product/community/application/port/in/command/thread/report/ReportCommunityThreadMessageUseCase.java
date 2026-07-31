package com.umc.product.community.application.port.in.command.thread.report;

import com.umc.product.community.application.port.in.command.thread.report.dto.ReportCommunityThreadMessageCommand;
import com.umc.product.community.application.port.in.query.thread.report.dto.CommunityThreadMessageReportReceiptInfo;

public interface ReportCommunityThreadMessageUseCase {

    CommunityThreadMessageReportReceiptInfo report(ReportCommunityThreadMessageCommand command);
}
