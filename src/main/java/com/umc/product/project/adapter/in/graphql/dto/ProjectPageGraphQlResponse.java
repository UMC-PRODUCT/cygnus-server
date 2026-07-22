package com.umc.product.project.adapter.in.graphql.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.umc.product.global.graphql.dto.PageInfoGraphQlResponse;
import com.umc.product.project.application.port.in.query.dto.ProjectInfo;

public record ProjectPageGraphQlResponse(
    List<ProjectGraphQlResponse> content,
    PageInfoGraphQlResponse pageInfo
) {
    public static ProjectPageGraphQlResponse from(Page<ProjectInfo> page) {
        return new ProjectPageGraphQlResponse(
            page.getContent().stream().map(ProjectGraphQlResponse::from).toList(),
            PageInfoGraphQlResponse.from(page)
        );
    }
}
