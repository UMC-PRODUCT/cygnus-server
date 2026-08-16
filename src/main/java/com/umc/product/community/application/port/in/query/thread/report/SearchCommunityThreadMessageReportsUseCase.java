package com.umc.product.community.application.port.in.query.thread.report;

import com.umc.product.community.application.port.in.query.thread.report.dto.CommunityThreadMessageReportPageInfo;
import com.umc.product.community.application.port.in.query.thread.report.dto.SearchCommunityThreadMessageReportsQuery;

public interface SearchCommunityThreadMessageReportsUseCase {

    CommunityThreadMessageReportPageInfo search(SearchCommunityThreadMessageReportsQuery query);
}
