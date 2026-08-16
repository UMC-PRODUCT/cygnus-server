package com.umc.product.organization.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.global.security.annotation.Public;
import com.umc.product.organization.adapter.in.web.dto.response.umcproduct.UmcProductOrganizationChartResponse;
import com.umc.product.organization.application.port.in.query.GetUmcProductOrganizationChartUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Public
@RestController
@RequestMapping("/api/v1/umc-product/organization-chart")
@RequiredArgsConstructor
@Tag(name = "Organization | UMC PRODUCT 조직도 Query", description = "현재 UMC PRODUCT 조직도를 조회합니다.")
public class UmcProductOrganizationChartQueryController {

    private final GetUmcProductOrganizationChartUseCase getUmcProductOrganizationChartUseCase;

    @GetMapping
    @Operation(
        operationId = "UMC-PRODUCT-ORGANIZATION-CHART-101",
        summary = "UMC PRODUCT 조직도 조회",
        description = "활성 Chapter와 하위 활성 Part, KST 오늘 날짜에 유효한 Squad를 반환합니다."
    )
    public UmcProductOrganizationChartResponse get() {
        return UmcProductOrganizationChartResponse.from(getUmcProductOrganizationChartUseCase.getCurrent());
    }
}
