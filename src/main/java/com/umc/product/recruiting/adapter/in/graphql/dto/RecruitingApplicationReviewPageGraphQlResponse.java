package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationSummaryInfo;

public record RecruitingApplicationReviewPageGraphQlResponse(
    List<RecruitingApplicationReviewGraphQlResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {

    public static RecruitingApplicationReviewPageGraphQlResponse from(Page<RecruitingApplicationSummaryInfo> page) {
        return new RecruitingApplicationReviewPageGraphQlResponse(
            page.getContent().stream().map(RecruitingApplicationReviewGraphQlResponse::from).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasNext()
        );
    }
}
