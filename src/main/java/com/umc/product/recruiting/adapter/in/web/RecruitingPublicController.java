package com.umc.product.recruiting.adapter.in.web;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.global.security.annotation.Public;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingApplicationFormResponse;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recruiting/public")
@Validated
@Tag(name = "Recruiting | 공개 모집", description = "지원자가 공개 모집 폼과 지원 결과를 조회합니다.")
@RequiredArgsConstructor
public class RecruitingPublicController {

    private final GetRecruitingFormQueryUseCase getRecruitingFormQueryUseCase;

    @GetMapping("/forms")
    @Public
    @Operation(
        operationId = "RECRUITING-PUBLIC-001",
        summary = "공개 지원 폼 목록 조회",
        description = "기수와 학교 기준으로 현재 공개된 리크루팅 지원 폼 목록을 조회합니다."
    )
    public List<RecruitingApplicationFormResponse> listPublicForms(
        @RequestParam @Positive Long gisuId,
        @RequestParam @Positive Long schoolId
    ) {
        return getRecruitingFormQueryUseCase.listPublicForms(gisuId, schoolId)
            .stream()
            .map(RecruitingApplicationFormResponse::from)
            .toList();
    }

}
