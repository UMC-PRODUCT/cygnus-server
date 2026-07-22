package com.umc.product.recruiting.adapter.in.graphql.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.umc.product.global.graphql.dto.PageInfoGraphQlResponse;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationSummaryInfo;

public record RecruitingApplicationPageGraphQlResponse(
    List<RecruitingApplicationGraphQlResponse> content,
    PageInfoGraphQlResponse pageInfo
) {

    public static RecruitingApplicationPageGraphQlResponse from(
        Page<RecruitingApplicationSummaryInfo> page,
        Long roundId
    ) {
        return new RecruitingApplicationPageGraphQlResponse(
            page.getContent().stream()
                .map(info -> RecruitingApplicationGraphQlResponse.from(info, roundId))
                .toList(),
            PageInfoGraphQlResponse.from(page)
        );
    }
}
