package com.umc.product.organization.adapter.in.graphql.dto;

import java.util.List;

import com.umc.product.organization.application.port.in.query.dto.gisu.GisuOrganizationInfo;

public record GisuOrganizationPayloadGraphQlResponse(
    List<GisuGraphQlResponse> gisus
) {

    public static GisuOrganizationPayloadGraphQlResponse from(List<GisuOrganizationInfo> infos) {
        return new GisuOrganizationPayloadGraphQlResponse(
            infos.stream()
                .map(GisuGraphQlResponse::from)
                .toList()
        );
    }
}
