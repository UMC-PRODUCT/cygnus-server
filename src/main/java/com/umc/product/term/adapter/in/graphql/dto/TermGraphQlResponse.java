package com.umc.product.term.adapter.in.graphql.dto;

import java.time.Instant;

import com.umc.product.term.application.port.in.query.dto.ActiveTermInfo;
import com.umc.product.term.application.port.in.query.dto.TermInfo;
import com.umc.product.term.domain.enums.TermType;

public record TermGraphQlResponse(
    Long id,
    TermType type,
    String typeDescription,
    String link,
    boolean mandatory,
    Long version,
    Instant createdAt,
    Instant updatedAt
) {

    public static TermGraphQlResponse from(ActiveTermInfo info) {
        return new TermGraphQlResponse(
            info.id(),
            info.type(),
            info.typeDescription(),
            info.link(),
            info.isMandatory(),
            info.version(),
            info.createdAt(),
            info.updatedAt()
        );
    }

    public static TermGraphQlResponse from(TermInfo info) {
        return new TermGraphQlResponse(
            info.id(),
            info.type(),
            info.type().getDescription(),
            info.link(),
            info.isMandatory(),
            null,
            null,
            null
        );
    }
}
