package com.umc.product.member.adapter.in.graphql.dto;

import java.time.Instant;

import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

public record MemberGisuGraphQlResponse(
    Long gisuId,
    Long generation,
    String startAt,
    String endAt,
    boolean active
) {

    public static MemberGisuGraphQlResponse from(GisuInfo info) {
        return new MemberGisuGraphQlResponse(
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
