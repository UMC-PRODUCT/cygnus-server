package com.umc.product.organization.adapter.in.graphql.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.umc.product.global.graphql.dto.PageInfoGraphQlResponse;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductMemberInfo;

public record UmcProductMemberPageGraphQlResponse(
    List<UmcProductMemberInfo> content,
    PageInfoGraphQlResponse pageInfo
) {

    public static UmcProductMemberPageGraphQlResponse from(Page<UmcProductMemberInfo> page) {
        return new UmcProductMemberPageGraphQlResponse(
            page.getContent(),
            PageInfoGraphQlResponse.from(page)
        );
    }
}
