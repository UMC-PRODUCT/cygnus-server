package com.umc.product.recruiting.adapter.in.web;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.response.PageResponse;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingApplicationDetailResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingApplicationSummaryResponse;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationSearchQuery;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recruiting/rounds/{roundId}/applications")
@Tag(name = "Recruiting | 지원서 평가 조회", description = "Round 평가자와 모집 운영진이 지원서를 조회합니다.")
@RequiredArgsConstructor
public class RecruitingApplicationReviewController {

    private final SearchRecruitingApplicationUseCase searchApplicationUseCase;

    @GetMapping
    @Operation(
        operationId = "RECRUITING-EVALUATION-003",
        summary = "평가용 지원서 목록 조회",
        description = "Round의 제출된 지원서를 상태·지원 트랙으로 필터링하고 본인의 단계별 평가 여부와 함께 조회합니다."
    )
    public PageResponse<RecruitingApplicationSummaryResponse> search(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long roundId,
        @RequestParam(required = false) RecruitingApplicationStatus status,
        @RequestParam(required = false) ChallengerTrack track,
        @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return PageResponse.of(
            searchApplicationUseCase.search(RecruitingApplicationSearchQuery.builder()
                .roundId(roundId)
                .status(status)
                .track(track)
                .requesterMemberId(memberPrincipal.getMemberId())
                .pageable(pageable)
                .build()),
            RecruitingApplicationSummaryResponse::from
        );
    }

    @GetMapping("/{applicationId}")
    @Operation(
        operationId = "RECRUITING-EVALUATION-004",
        summary = "평가용 지원서 상세 조회",
        description = "지원자 기본 정보와 Form 답변을 조회합니다. 초안은 평가자에게 공개하지 않습니다."
    )
    public RecruitingApplicationDetailResponse getDetail(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long roundId,
        @PathVariable @Positive Long applicationId
    ) {
        return RecruitingApplicationDetailResponse.from(
            searchApplicationUseCase.getDetail(roundId, applicationId, memberPrincipal.getMemberId())
        );
    }
}
