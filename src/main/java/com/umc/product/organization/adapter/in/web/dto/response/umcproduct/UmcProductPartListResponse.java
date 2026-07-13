package com.umc.product.organization.adapter.in.web.dto.response.umcproduct;

import java.util.List;

import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductPartInfo;

public record UmcProductPartListResponse(
    List<UmcProductPartResponse> parts
) {
    public static UmcProductPartListResponse from(List<UmcProductPartInfo> infos) {
        return new UmcProductPartListResponse(infos.stream().map(UmcProductPartResponse::from).toList());
    }
}
