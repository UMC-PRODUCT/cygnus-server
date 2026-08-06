package com.umc.product.community.application.port.out.report.dto;

import java.util.List;

import com.umc.product.community.domain.Report;

public record ThreadMessageReportSearchResult(List<Report> reports, long total) {

    public ThreadMessageReportSearchResult {
        reports = reports == null ? List.of() : List.copyOf(reports);
        if (total < 0) {
            throw new IllegalArgumentException("total must not be negative");
        }
    }
}
