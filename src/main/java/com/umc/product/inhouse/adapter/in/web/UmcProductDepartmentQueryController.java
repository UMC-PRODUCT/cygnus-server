package com.umc.product.inhouse.adapter.in.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.global.security.annotation.Public;
import com.umc.product.inhouse.adapter.in.web.dto.response.UmcProductDepartmentListResponse;
import com.umc.product.inhouse.application.port.in.query.GetUmcProductDepartmentUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Public
@RestController
@RequestMapping("/api/v1/umc-product/departments")
@RequiredArgsConstructor
@Tag(name = "Organization | UMC PRODUCT Department Query", description = "UMC PRODUCT Department 목록을 조회합니다.")
public class UmcProductDepartmentQueryController {

    private final GetUmcProductDepartmentUseCase getUmcProductDepartmentUseCase;

    @GetMapping
    @Operation(
        operationId = "UMC-PRODUCT-DEPARTMENT-101",
        summary = "UMC PRODUCT Department 목록 조회",
        description = "active로 활성 상태를, activeOn으로 해당 날짜에 유효한 Department를 필터링합니다."
    )
    public UmcProductDepartmentListResponse list(
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false)
        @DateTimeFormat(pattern = "uuuu-MM-dd")
        @Parameter(schema = @Schema(type = "string", format = "date", example = "2026-07-13"))
        LocalDate activeOn
    ) {
        return UmcProductDepartmentListResponse.from(getUmcProductDepartmentUseCase.list(active, activeOn));
    }
}
