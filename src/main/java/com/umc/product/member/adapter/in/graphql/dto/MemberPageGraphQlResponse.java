package com.umc.product.member.adapter.in.graphql.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.umc.product.global.graphql.dto.PageInfoGraphQlResponse;
import com.umc.product.member.application.port.in.query.dto.SearchMemberItemV2Info;

public record MemberPageGraphQlResponse(
    List<MemberSearchResultGraphQlResponse> content,
    PageInfoGraphQlResponse pageInfo
) {

    public static MemberPageGraphQlResponse from(Page<SearchMemberItemV2Info> page) {
        return new MemberPageGraphQlResponse(
            page.getContent().stream().map(MemberSearchResultGraphQlResponse::from).toList(),
            PageInfoGraphQlResponse.from(page)
        );
    }
}
