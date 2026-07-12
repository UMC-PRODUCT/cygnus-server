package com.umc.product.recruiting.adapter.in.web;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.web.dto.request.CreateRecruitingRoundRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.CreateRecruitingSeasonRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.ReplaceRecruitingSeasonTrackQuotasRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.UpdateRecruitingRoundRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.UpdateRecruitingRoundStatusRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.UpdateRecruitingSeasonStatusRequest;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingIdResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingSeasonConfigurationResponse;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.command.ReplaceRecruitingSeasonTrackQuotasUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingSeasonStatusUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingSeasonConfigurationUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recruiting/admin")
@Validated
@Tag(name = "Recruiting | 시즌 관리", description = "운영진이 모집 시즌, 쿼터와 차수를 관리합니다.")
@RequiredArgsConstructor
public class RecruitingSeasonAdminController {

    private final CreateRecruitingSeasonUseCase createSeasonUseCase;
    private final UpdateRecruitingSeasonStatusUseCase updateSeasonStatusUseCase;
    private final ReplaceRecruitingSeasonTrackQuotasUseCase replaceSeasonTrackQuotasUseCase;
    private final CreateRecruitingRoundUseCase createRoundUseCase;
    private final UpdateRecruitingRoundStatusUseCase updateRoundStatusUseCase;
    private final UpdateRecruitingRoundUseCase updateRoundUseCase;
    private final GetRecruitingSeasonConfigurationUseCase getSeasonConfigurationUseCase;

    @GetMapping("/seasons/{seasonId}")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.READ)
    @Operation(
        operationId = "RECRUITING-ADMIN-000",
        summary = "모집 시즌 설정 조회",
        description = "시즌의 트랙별 목표 인원과 차수별 모집 설정을 조회합니다."
    )
    public RecruitingSeasonConfigurationResponse getSeasonConfiguration(
        @PathVariable @Positive Long seasonId
    ) {
        return RecruitingSeasonConfigurationResponse.from(getSeasonConfigurationUseCase.getBySeasonId(seasonId));
    }

    @PostMapping("/seasons")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, permission = PermissionType.WRITE)
    @Operation(
        operationId = "RECRUITING-ADMIN-001",
        summary = "모집 시즌 생성",
        description = "기수와 학교에 대한 모집 시즌과 초기 트랙별 목표 인원을 생성합니다."
    )
    public RecruitingIdResponse createSeason(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @Valid @RequestBody CreateRecruitingSeasonRequest request
    ) {
        return RecruitingIdResponse.from(
            createSeasonUseCase.createSeason(request.toCommand(memberPrincipal.getMemberId()))
        );
    }

    @PatchMapping("/seasons/{seasonId}/status")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    @Operation(
        operationId = "RECRUITING-ADMIN-002",
        summary = "모집 시즌 상태 변경",
        description = "모집 시즌의 운영 상태를 변경합니다."
    )
    public void updateSeasonStatus(
        @PathVariable @Positive Long seasonId,
        @Valid @RequestBody UpdateRecruitingSeasonStatusRequest request
    ) {
        updateSeasonStatusUseCase.updateSeasonStatus(request.toCommand(seasonId));
    }

    @PutMapping("/seasons/{seasonId}/quotas")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    @Operation(
        operationId = "RECRUITING-ADMIN-002A",
        summary = "모집 시즌 트랙별 목표 인원 교체",
        description = "현재 READY 및 REGISTERED 인원을 보호하면서 트랙별 목표 인원을 교체합니다."
    )
    public void replaceSeasonTrackQuotas(
        @PathVariable @Positive Long seasonId,
        @Valid @RequestBody ReplaceRecruitingSeasonTrackQuotasRequest request
    ) {
        replaceSeasonTrackQuotasUseCase.replaceQuotas(request.toCommand(seasonId));
    }

    @PostMapping("/seasons/{seasonId}/rounds")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.WRITE)
    @Operation(
        operationId = "RECRUITING-ADMIN-003",
        summary = "모집 차수 생성",
        description = "모집 기간, 트랙, 2지망 정책과 면접 설정을 포함한 차수를 생성합니다."
    )
    public RecruitingIdResponse createRound(
        @PathVariable @Positive Long seasonId,
        @Valid @RequestBody CreateRecruitingRoundRequest request
    ) {
        return RecruitingIdResponse.from(createRoundUseCase.createRound(request.toCommand(seasonId)));
    }

    @PatchMapping("/seasons/{seasonId}/rounds/{roundId}/status")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    @Operation(
        operationId = "RECRUITING-ADMIN-004",
        summary = "모집 차수 상태 변경",
        description = "모집 차수의 운영 상태를 변경합니다."
    )
    public void updateRoundStatus(
        @PathVariable @Positive Long seasonId,
        @PathVariable @Positive Long roundId,
        @Valid @RequestBody UpdateRecruitingRoundStatusRequest request
    ) {
        updateRoundStatusUseCase.updateRoundStatus(request.toCommand(seasonId, roundId));
    }

    @PutMapping("/seasons/{seasonId}/rounds/{roundId}")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    @Operation(
        operationId = "RECRUITING-ADMIN-004A",
        summary = "모집 차수 설정 변경",
        description = "모집 기간, 트랙, 2지망 정책과 면접 설정을 변경합니다."
    )
    public void updateRound(
        @PathVariable @Positive Long seasonId,
        @PathVariable @Positive Long roundId,
        @Valid @RequestBody UpdateRecruitingRoundRequest request
    ) {
        updateRoundUseCase.updateRound(request.toCommand(seasonId, roundId));
    }
}
