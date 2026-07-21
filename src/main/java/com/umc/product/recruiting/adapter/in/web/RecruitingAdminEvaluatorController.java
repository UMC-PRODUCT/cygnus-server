package com.umc.product.recruiting.adapter.in.web;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingIdResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingRoundEvaluatorResponse;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingRoundEvaluatorUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingRoundEvaluatorCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingRoundEvaluatorUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recruiting/admin/rounds/{roundId}/evaluators")
@Validated
@Tag(name = "Recruiting | 평가자 관리", description = "운영진이 모집 차수의 평가자 whitelist를 관리합니다.")
@RequiredArgsConstructor
public class RecruitingAdminEvaluatorController {

    private final ManageRecruitingRoundEvaluatorUseCase manageEvaluatorUseCase;
    private final GetRecruitingRoundEvaluatorUseCase getEvaluatorUseCase;

    @PostMapping("/{memberId}")
    @Operation(
        operationId = "RECRUITING-ADMIN-031",
        summary = "평가자 추가",
        description = "CurrentMember 운영 권한으로 path의 회원을 서류·면접 공통 평가자 whitelist에 추가합니다."
    )
    public RecruitingIdResponse addEvaluator(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long roundId,
        @PathVariable @Positive Long memberId
    ) {
        return RecruitingIdResponse.from(manageEvaluatorUseCase.addEvaluator(
            RecruitingRoundEvaluatorCommand.of(roundId, memberPrincipal.getMemberId(), memberId)
        ));
    }

    @DeleteMapping("/{memberId}")
    @Operation(
        operationId = "RECRUITING-ADMIN-032",
        summary = "평가자 제거",
        description = "CurrentMember 운영 권한으로 서류·면접 공통 평가자 whitelist 등록을 제거합니다."
    )
    public void removeEvaluator(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long roundId,
        @PathVariable @Positive Long memberId
    ) {
        manageEvaluatorUseCase.removeEvaluator(
            RecruitingRoundEvaluatorCommand.of(roundId, memberPrincipal.getMemberId(), memberId)
        );
    }

    @GetMapping
    @Operation(
        operationId = "RECRUITING-ADMIN-033",
        summary = "평가자 목록 조회",
        description = "모집 차수의 서류·면접 공통 평가자 whitelist를 조회합니다."
    )
    public List<RecruitingRoundEvaluatorResponse> listEvaluators(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long roundId
    ) {
        return getEvaluatorUseCase.listByRoundId(roundId, memberPrincipal.getMemberId())
            .stream()
            .map(RecruitingRoundEvaluatorResponse::from)
            .toList();
    }
}
