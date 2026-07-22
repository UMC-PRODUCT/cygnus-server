package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.umc.product.global.graphql.dto.PageInfoGraphQlResponse;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationSummaryInfo;

public record RecruitingApplicationReviewPageGraphQlResponse(
    List<RecruitingApplicationReviewGraphQlResponse> content,
    PageInfoGraphQlResponse pageInfo
) {

    public static RecruitingApplicationReviewPageGraphQlResponse from(Page<RecruitingApplicationSummaryInfo> page) {
        return new RecruitingApplicationReviewPageGraphQlResponse(
            page.getContent().stream().map(RecruitingApplicationReviewGraphQlResponse::from).toList(),
            PageInfoGraphQlResponse.from(page)
        );
    }
}
