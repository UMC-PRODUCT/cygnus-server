package com.umc.product.organization.adapter.in.graphql.dto;

import java.time.Instant;

import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuOrganizationInfo;

public record GisuGraphQlResponse(
    Long id,
    Long generation,
    Instant startAt,
    Instant endAt,
    boolean active
) {

    public static GisuGraphQlResponse from(GisuInfo info) {
        return new GisuGraphQlResponse(
            info.gisuId(),
            info.generation(),
            info.startAt(),
            info.endAt(),
            info.isActive()
        );
    }

    public static GisuGraphQlResponse from(GisuOrganizationInfo info) {
        return new GisuGraphQlResponse(
            info.gisuId(),
            info.generation(),
            info.startAt(),
            info.endAt(),
            info.isActive()
        );
    }

}
