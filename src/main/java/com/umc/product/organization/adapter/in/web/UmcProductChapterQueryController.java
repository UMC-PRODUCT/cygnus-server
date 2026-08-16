package com.umc.product.organization.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.global.security.annotation.Public;
import com.umc.product.organization.adapter.in.web.dto.response.umcproduct.UmcProductChapterListResponse;
import com.umc.product.organization.application.port.in.query.GetUmcProductChapterUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Public
@RestController
@RequestMapping("/api/v1/umc-product/chapters")
@RequiredArgsConstructor
@Tag(name = "Organization | UMC PRODUCT Chapter Query", description = "UMC PRODUCT Chapter를 조회합니다.")
public class UmcProductChapterQueryController {

    private final GetUmcProductChapterUseCase getUmcProductChapterUseCase;

    @GetMapping
    @Operation(operationId = "UMC-PRODUCT-CHAPTER-101", summary = "UMC PRODUCT Chapter 목록 조회")
    public UmcProductChapterListResponse list(@RequestParam(required = false) Boolean active) {
        return UmcProductChapterListResponse.from(getUmcProductChapterUseCase.list(active));
    }
}
