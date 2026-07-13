package com.umc.product.organization.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.global.security.annotation.Public;
import com.umc.product.organization.adapter.in.web.dto.response.umcproduct.UmcProductPartListResponse;
import com.umc.product.organization.application.port.in.query.GetUmcProductPartUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Public
@RestController
@RequestMapping("/api/v1/umc-product/parts")
@RequiredArgsConstructor
@Tag(name = "Organization | UMC PRODUCT Part Query", description = "UMC PRODUCT Part를 조회합니다.")
public class UmcProductPartQueryController {

    private final GetUmcProductPartUseCase getUmcProductPartUseCase;

    @GetMapping
    @Operation(operationId = "UMC-PRODUCT-PART-101", summary = "UMC PRODUCT Part 목록 조회")
    public UmcProductPartListResponse list(
        @RequestParam(required = false) Long chapterId,
        @RequestParam(required = false) Boolean active
    ) {
        return UmcProductPartListResponse.from(getUmcProductPartUseCase.list(chapterId, active));
    }
}
