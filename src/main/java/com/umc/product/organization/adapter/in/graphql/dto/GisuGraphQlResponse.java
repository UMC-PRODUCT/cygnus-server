package com.umc.product.organization.adapter.in.graphql.dto;

import java.time.Instant;

import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.RelayNode;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuOrganizationInfo;

/**
 * 스키마 {@code Gisu} 타입 응답. gisuId는 배치 로딩용 raw ID로 스키마에 노출하지 않는다.
 */
public record GisuGraphQlResponse(
    Long gisuId,
    int generation,
    String startAt,
    String endAt,
    boolean active
) implements RelayNode {

    public static GisuGraphQlResponse from(GisuInfo info) {
        return new GisuGraphQlResponse(
            info.gisuId(),
            Math.toIntExact(info.generation()),
            format(info.startAt()),
            format(info.endAt()),
            info.isActive()
        );
    }

    public static GisuGraphQlResponse from(GisuOrganizationInfo info) {
        return new GisuGraphQlResponse(
            info.gisuId(),
            Math.toIntExact(info.generation()),
            format(info.startAt()),
            format(info.endAt()),
            info.isActive()
        );
    }

    @Override
    public String id() {
        return GlobalId.encode(GlobalIdTypes.GISU, gisuId);
    }

    private static String format(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
