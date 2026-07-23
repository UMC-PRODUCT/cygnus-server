package com.umc.product.community.adapter.in.graphql.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.umc.product.community.application.port.in.query.dto.PostInfo;
import com.umc.product.global.graphql.dto.PageInfoGraphQlResponse;

public record CommunityPostPageGraphQlResponse(
    List<CommunityPostGraphQlResponse> content,
    PageInfoGraphQlResponse pageInfo
) {

    public static CommunityPostPageGraphQlResponse from(Page<PostInfo> page) {
        return new CommunityPostPageGraphQlResponse(
            page.getContent().stream().map(CommunityPostGraphQlResponse::from).toList(),
            PageInfoGraphQlResponse.from(page)
        );
    }
}
