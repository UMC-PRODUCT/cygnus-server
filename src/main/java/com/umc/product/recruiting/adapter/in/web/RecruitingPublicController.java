package com.umc.product.recruiting.adapter.in.web;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingApplicationFormResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingApplicationResultResponse;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recruiting/public")
@Tag(name = "Recruiting | 공개 모집", description = "지원자가 공개 모집 폼과 지원 결과를 조회합니다.")
@RequiredArgsConstructor
public class RecruitingPublicController {

    private final GetRecruitingFormQueryUseCase getRecruitingFormQueryUseCase;
    private final GetRecruitingApplicationQueryUseCase getRecruitingApplicationQueryUseCase;

    @GetMapping("/forms")
    @Operation(
        operationId = "RECRUITING-PUBLIC-001",
        summary = "공개 지원 폼 목록 조회",
        description = "기수와 학교 기준으로 현재 공개된 리크루팅 지원 폼 목록을 조회합니다."
    )
    public List<RecruitingApplicationFormResponse> listPublicForms(
        @RequestParam Long gisuId,
        @RequestParam Long schoolId
    ) {
        return getRecruitingFormQueryUseCase.listPublicForms(gisuId, schoolId)
            .stream()
            .map(RecruitingApplicationFormResponse::from)
            .toList();
    }

    @GetMapping("/applications/result")
    @Operation(
        operationId = "RECRUITING-PUBLIC-002",
        summary = "익명 지원 결과 조회",
        description = "지원서 번호와 익명 식별 키로 지원 결과와 안내 상태를 조회합니다."
    )
    public RecruitingApplicationResultResponse getAnonymousResult(
        @ParameterObject AnonymousResultRequest request
    ) {
        return RecruitingApplicationResultResponse.from(
            getRecruitingApplicationQueryUseCase.getAnonymousResult(
                request.applicationNo(),
                request.applicantIdentityKey()
            )
        );
    }

    @Schema(description = "익명 지원 결과 조회 조건")
    public record AnonymousResultRequest(
        @Schema(description = "지원서 제출 후 발급된 고유 지원서 번호", example = "REC-2026-0001")
        String applicationNo,
        @Schema(description = "지원자 본인 확인을 위한 익명 식별 키", example = "identity-key")
        String applicantIdentityKey
    ) {
    }
}
