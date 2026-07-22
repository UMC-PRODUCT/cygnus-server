package com.umc.product.member.adapter.in.graphql.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.umc.product.global.graphql.dto.PageInfoGraphQlResponse;
import com.umc.product.member.application.port.in.query.dto.SearchMemberItemV2Info;

public record MemberSearchPageGraphQlResponse(
    List<MemberSearchEdgeGraphQlResponse> content,
    PageInfoGraphQlResponse pageInfo
) {

    public static MemberSearchPageGraphQlResponse from(Page<SearchMemberItemV2Info> page) {
        return new MemberSearchPageGraphQlResponse(
            page.getContent().stream().map(MemberSearchEdgeGraphQlResponse::from).toList(),
            PageInfoGraphQlResponse.from(page)
        );
    }
}
