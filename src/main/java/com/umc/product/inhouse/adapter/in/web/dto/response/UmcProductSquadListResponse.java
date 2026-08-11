package com.umc.product.inhouse.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductSquadInfo;

public record UmcProductSquadListResponse(
    List<UmcProductSquadResponse> squads
) {
    public static UmcProductSquadListResponse from(List<UmcProductSquadInfo> infos) {
        return new UmcProductSquadListResponse(
            infos.stream().map(UmcProductSquadResponse::from).toList()
        );
    }
}
