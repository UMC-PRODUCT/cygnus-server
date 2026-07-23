package com.umc.product.community.application.port.out.report;

import com.umc.product.community.application.port.out.report.dto.ThreadMessageReportSearchQuery;
import com.umc.product.community.application.port.out.report.dto.ThreadMessageReportSearchResult;

public interface SearchThreadMessageReportPort {

    ThreadMessageReportSearchResult search(ThreadMessageReportSearchQuery query);
}
