package com.umc.product.challenger.adapter.in.graphql.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.global.graphql.dto.PageInfoGraphQlResponse;

public record ChallengerPageGraphQlResponse(
    List<ChallengerGraphQlResponse> content,
    PageInfoGraphQlResponse pageInfo
) {

    public static ChallengerPageGraphQlResponse from(Page<ChallengerInfo> page) {
        return new ChallengerPageGraphQlResponse(
            page.getContent().stream().map(ChallengerGraphQlResponse::from).toList(),
            PageInfoGraphQlResponse.from(page)
        );
    }
}
