package com.umc.product.organization.adapter.in.web;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.global.response.PageResponse;
import com.umc.product.global.security.annotation.Public;
import com.umc.product.organization.adapter.in.web.dto.response.umcproduct.UmcProductMemberPageResponse;
import com.umc.product.organization.adapter.in.web.dto.response.umcproduct.UmcProductMemberResponse;
import com.umc.product.organization.application.port.in.query.GetUmcProductMemberUseCase;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductMemberSearchCondition;
import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;
import com.umc.product.organization.domain.enums.UmcProductPosition;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Public
@RestController
@RequestMapping("/api/v1/umc-product/members")
@RequiredArgsConstructor
@Tag(name = "Organization | UMC PRODUCT 멤버 Query", description = "UMC PRODUCT 멤버 목록과 상세 정보를 조회합니다.")
public class UmcProductMemberQueryController {

    private final GetUmcProductMemberUseCase getUmcProductMemberUseCase;

    @GetMapping
    @Operation(
        operationId = "UMC-PRODUCT-MEMBER-101",
        summary = "UMC PRODUCT 멤버 검색",
        description = "Chapter, Product Leadership, 포지션, Squad, 활동 기준일로 멤버를 페이지 조회합니다."
    )
    public UmcProductMemberPageResponse search(
        @RequestParam(required = false) Long chapterId,
        @RequestParam(required = false) UmcProductLeadershipRole leadershipRole,
        @RequestParam(required = false) UmcProductPosition position,
        @RequestParam(required = false) Long squadId,
        @RequestParam(required = false)
        @DateTimeFormat(pattern = "uuuu-MM-dd")
        @Parameter(schema = @Schema(type = "string", format = "date", example = "2026-07-13"))
        LocalDate activeOn,
        Pageable pageable
    ) {
        UmcProductMemberSearchCondition condition = UmcProductMemberSearchCondition.of(
            chapterId,
            leadershipRole,
            position,
            squadId,
            activeOn
        );
        PageResponse<UmcProductMemberResponse> pageResponse = PageResponse.of(
            getUmcProductMemberUseCase.search(condition, pageable),
            UmcProductMemberResponse::from
        );
        return UmcProductMemberPageResponse.from(pageResponse);
    }

    @GetMapping("/{memberId}")
    @Operation(
        operationId = "UMC-PRODUCT-MEMBER-102",
        summary = "UMC PRODUCT 멤버 상세 조회",
        description = "멤버 기본 정보와 활동 기간, Chapter 소속, Product Leadership, Squad 참여 이력을 반환합니다."
    )
    public UmcProductMemberResponse get(@PathVariable Long memberId) {
        return UmcProductMemberResponse.from(getUmcProductMemberUseCase.getById(memberId));
    }
}
