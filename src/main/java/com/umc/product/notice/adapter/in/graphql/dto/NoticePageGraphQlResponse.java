package com.umc.product.notice.adapter.in.graphql.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.umc.product.global.graphql.dto.PageInfoGraphQlResponse;
import com.umc.product.notice.application.port.in.query.dto.NoticeSummary;

public record NoticePageGraphQlResponse(
    List<NoticeGraphQlResponse> content,
    PageInfoGraphQlResponse pageInfo
) {

    public static NoticePageGraphQlResponse from(Page<NoticeSummary> page) {
        return new NoticePageGraphQlResponse(
            page.getContent().stream().map(NoticeGraphQlResponse::from).toList(),
            PageInfoGraphQlResponse.from(page)
        );
    }
}
