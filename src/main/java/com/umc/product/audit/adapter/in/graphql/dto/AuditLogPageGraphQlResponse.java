package com.umc.product.audit.adapter.in.graphql.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.umc.product.audit.application.port.in.query.dto.AuditLogInfo;
import com.umc.product.global.graphql.dto.PageInfoGraphQlResponse;

public record AuditLogPageGraphQlResponse(
    List<AuditLogInfo> content,
    PageInfoGraphQlResponse pageInfo
) {

    public static AuditLogPageGraphQlResponse from(Page<AuditLogInfo> page) {
        return new AuditLogPageGraphQlResponse(page.getContent(), PageInfoGraphQlResponse.from(page));
    }
}
