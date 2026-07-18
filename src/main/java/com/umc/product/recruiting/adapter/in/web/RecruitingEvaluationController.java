package com.umc.product.recruiting.adapter.in.web;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.web.dto.request.SubmitRecruitingEvaluationRequest;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingEvaluationResponse;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingApplicationEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.query.ValidateRecruitingApplicationScopeUseCase;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recruiting/rounds/{roundId}/applications/{applicationId}/evaluations/{stage}")
@Validated
@Tag(name = "Recruiting | 평가", description = "평가자가 단계별 지원서 평가를 확정하고 조회합니다.")
@RequiredArgsConstructor
public class RecruitingEvaluationController {

    private final SubmitRecruitingApplicationEvaluationUseCase submitEvaluationUseCase;
    private final GetRecruitingApplicationEvaluationUseCase getEvaluationUseCase;
    private final ValidateRecruitingApplicationScopeUseCase validateApplicationScopeUseCase;

    @PostMapping
    @Operation(
        operationId = "RECRUITING-EVALUATION-001",
        summary = "평가 확정",
        description = "CurrentMember 평가자의 단계별 평가를 생성 즉시 수정 불가 상태로 확정합니다."
    )
    public void submit(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long roundId,
        @PathVariable @Positive Long applicationId,
        @PathVariable RecruitingEvaluatorStage stage,
        @Valid @RequestBody SubmitRecruitingEvaluationRequest request
    ) {
        validateApplicationScopeUseCase.validateRoundScope(applicationId, roundId);
        submitEvaluationUseCase.submit(
            request.toSubmitCommand(applicationId, memberPrincipal.getMemberId(), stage)
        );
    }

    @GetMapping
    @Operation(
        operationId = "RECRUITING-EVALUATION-002",
        summary = "평가 목록 조회",
        description = "CurrentMember에게 공개 가능한 단계별 평가 목록만 조회합니다."
    )
    public List<RecruitingEvaluationResponse> listVisible(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long roundId,
        @PathVariable @Positive Long applicationId,
        @PathVariable RecruitingEvaluatorStage stage
    ) {
        validateApplicationScopeUseCase.validateRoundScope(applicationId, roundId);
        return getEvaluationUseCase.listVisibleEvaluations(
            applicationId,
            memberPrincipal.getMemberId(),
            stage
        ).stream().map(RecruitingEvaluationResponse::from).toList();
    }
}
