package com.umc.product.organization.adapter.in.graphql.dto;

import java.time.Instant;

import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuOrganizationInfo;

public record GisuGraphQlResponse(
    Long gisuId,
    Long generation,
    String startAt,
    String endAt,
    boolean active
) {

    public static GisuGraphQlResponse from(GisuInfo info) {
        return new GisuGraphQlResponse(
            info.gisuId(),
            info.generation(),
            format(info.startAt()),
            format(info.endAt()),
            info.isActive()
        );
    }

    public static GisuGraphQlResponse from(GisuOrganizationInfo info) {
        return new GisuGraphQlResponse(
            info.gisuId(),
            info.generation(),
            format(info.startAt()),
            format(info.endAt()),
            info.isActive()
        );
    }

    private static String format(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
