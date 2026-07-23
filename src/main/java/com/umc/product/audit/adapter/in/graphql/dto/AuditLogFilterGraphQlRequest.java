package com.umc.product.audit.adapter.in.graphql.dto;

import java.time.Instant;

import com.umc.product.audit.application.port.in.query.dto.SearchAuditLogQuery;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.global.exception.constant.Domain;

public record AuditLogFilterGraphQlRequest(
    Domain domain,
    AuditAction action,
    Long actorMemberId,
    Instant from,
    Instant to
) {

    public SearchAuditLogQuery toQuery() {
        return new SearchAuditLogQuery(domain, action, actorMemberId, from, to);
    }

    public static AuditLogFilterGraphQlRequest empty() {
        return new AuditLogFilterGraphQlRequest(null, null, null, null, null);
    }
}
