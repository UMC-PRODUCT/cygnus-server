package com.umc.product.inhouse.adapter.in.web.dto.response;

import java.util.List;

import com.umc.product.inhouse.application.port.in.query.dto.UmcProductDepartmentInfo;

public record UmcProductDepartmentListResponse(
    List<UmcProductDepartmentResponse> departments
) {
    public static UmcProductDepartmentListResponse from(List<UmcProductDepartmentInfo> infos) {
        return new UmcProductDepartmentListResponse(
            infos.stream().map(UmcProductDepartmentResponse::from).toList()
        );
    }
}
