package com.umc.product.recruiting.adapter.in.web;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.web.dto.request.CloneRecruitingRoundRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.CreateRecruitingRoundRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.CreateRecruitingSeasonRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.ReplaceRecruitingSeasonTrackQuotasRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.UpdateRecruitingRoundRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.UpdateRecruitingRoundStatusRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.UpdateRecruitingSeasonRequest;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingIdResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingRoundTitleAvailabilityResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingSeasonConfigurationResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingSeasonSummaryResponse;
import com.umc.product.recruiting.application.port.in.command.CloneRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.command.DeleteRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.ReplaceRecruitingSeasonTrackQuotasUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.DeleteRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.query.CheckRecruitingRoundTitleUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingSeasonConfigurationUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingRoundGroupUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundGroupSearchQuery;
import com.umc.product.recruiting.domain.enums.RecruitingRoundSort;

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
    private final UpdateRecruitingSeasonUseCase updateSeasonUseCase;
    private final ReplaceRecruitingSeasonTrackQuotasUseCase replaceSeasonTrackQuotasUseCase;
    private final CreateRecruitingRoundUseCase createRoundUseCase;
    private final UpdateRecruitingRoundStatusUseCase updateRoundStatusUseCase;
    private final UpdateRecruitingRoundUseCase updateRoundUseCase;
    private final GetRecruitingSeasonConfigurationUseCase getSeasonConfigurationUseCase;
    private final SearchRecruitingRoundGroupUseCase searchRoundGroupUseCase;
    private final CheckRecruitingRoundTitleUseCase checkRoundTitleUseCase;
    private final CloneRecruitingRoundUseCase cloneRoundUseCase;
    private final DeleteRecruitingRoundUseCase deleteRoundUseCase;

    @GetMapping("/rounds")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, permission = PermissionType.READ)
    @Operation(
        operationId = "RECRUITING-ADMIN-011",
        summary = "모집 차수 목록 조회",
        description = "권한이 있는 운영진이 편집·관리 화면에서 DRAFT, OPEN, CLOSED 모집을 Season별로 조회합니다. "
            + "공개 모집 탐색과 달리 Season 메모, 연락처와 Form 설정을 포함합니다."
    )
    public List<RecruitingSeasonSummaryResponse> searchRounds(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @Parameter(description = "조회할 기수 ID", example = "15")
        @RequestParam @Positive Long gisuId,
        @Parameter(description = "현재 학교 소속 기준 지부 ID", example = "2")
        @RequestParam(required = false) @Positive Long chapterId,
        @Parameter(description = "학교 ID", example = "3")
        @RequestParam(required = false) @Positive Long schoolId,
        @Parameter(description = "모집 시즌 ID", example = "10")
        @RequestParam(required = false) @Positive Long seasonId,
        @Parameter(description = "모집 트랙") @RequestParam(required = false) ChallengerTrack track,
        @Parameter(description = "정렬 기준") @RequestParam(required = false) RecruitingRoundSort sort
    ) {
        return searchRoundGroupUseCase.searchRoundGroups(RecruitingRoundGroupSearchQuery.builder()
                .gisuId(gisuId)
                .chapterId(chapterId)
                .schoolId(schoolId)
                .seasonId(seasonId)
                .track(track)
                .sort(sort)
                .requesterMemberId(memberPrincipal.getMemberId())
                .build())
            .stream()
            .map(RecruitingSeasonSummaryResponse::from)
            .toList();
    }

    @GetMapping("/seasons/{seasonId}/rounds/title-availability")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.READ)
    @Operation(
        operationId = "RECRUITING-ADMIN-012",
        summary = "모집 제목 사용 가능 여부 조회",
        description = "같은 시즌에서 대소문자를 무시한 모집 제목 중복 여부를 미리 확인합니다. "
            + "실제 생성·수정에서도 같은 검증을 반복하고 DB unique index가 최종 중복을 방어합니다."
    )
    public RecruitingRoundTitleAvailabilityResponse checkRoundTitle(
        @PathVariable @Positive Long seasonId,
        @RequestParam String title,
        @RequestParam(required = false) @Positive Long excludedRoundId
    ) {
        return new RecruitingRoundTitleAvailabilityResponse(
            checkRoundTitleUseCase.isTitleAvailable(seasonId, title, excludedRoundId)
        );
    }

    @GetMapping("/seasons/{seasonId}")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.READ)
    @Operation(
        operationId = "RECRUITING-ADMIN-001",
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
        operationId = "RECRUITING-ADMIN-002",
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

    @PatchMapping("/seasons/{seasonId}")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    @Operation(
        operationId = "RECRUITING-ADMIN-003",
        summary = "모집 시즌 수정",
        description = "시즌에 속한 운영진이 공유할 메모를 수정합니다."
    )
    public void updateSeason(
        @PathVariable @Positive Long seasonId,
        @Valid @RequestBody UpdateRecruitingSeasonRequest request
    ) {
        updateSeasonUseCase.updateSeason(request.toCommand(seasonId));
    }

    @PutMapping("/seasons/{seasonId}/quotas")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    @Operation(
        operationId = "RECRUITING-ADMIN-004",
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
        operationId = "RECRUITING-ADMIN-013",
        summary = "모집 차수 생성",
        description = "모집 기간, 트랙, 2지망 정책과 면접 설정을 포함한 차수를 생성합니다. "
            + "INFRA_PLUS는 모집할 수 없으며 면접 Round의 availability Form은 OPEN 전까지 설정·게시해야 합니다."
    )
    public RecruitingIdResponse createRound(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long seasonId,
        @Valid @RequestBody CreateRecruitingRoundRequest request
    ) {
        return RecruitingIdResponse.from(
            createRoundUseCase.createRound(request.toCommand(seasonId, memberPrincipal.getMemberId()))
        );
    }

    @PatchMapping("/seasons/{seasonId}/rounds/{roundId}/status")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    @Operation(
        operationId = "RECRUITING-ADMIN-015",
        summary = "모집 차수 상태 변경",
        description = "Round와 지원 Form 상태를 함께 변경합니다. 지원서와 Form 응답이 없는 OPEN Round만 DRAFT로 "
            + "되돌릴 수 있고, CLOSED는 다시 열거나 DRAFT로 되돌릴 수 없습니다."
    )
    public void updateRoundStatus(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long seasonId,
        @PathVariable @Positive Long roundId,
        @Valid @RequestBody UpdateRecruitingRoundStatusRequest request
    ) {
        updateRoundStatusUseCase.updateRoundStatus(
            request.toCommand(seasonId, roundId, memberPrincipal.getMemberId())
        );
    }

    @PutMapping("/seasons/{seasonId}/rounds/{roundId}")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    @Operation(
        operationId = "RECRUITING-ADMIN-014",
        summary = "모집 차수 설정 변경",
        description = "모집 기간, 트랙, 2지망 정책과 면접 설정을 변경합니다."
    )
    public void updateRound(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long seasonId,
        @PathVariable @Positive Long roundId,
        @Valid @RequestBody UpdateRecruitingRoundRequest request
    ) {
        updateRoundUseCase.updateRound(
            request.toCommand(seasonId, roundId, memberPrincipal.getMemberId())
        );
    }

    @PostMapping("/seasons/{seasonId}/rounds/{roundId}/clone")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.READ)
    @Operation(
        operationId = "RECRUITING-ADMIN-016",
        summary = "모집 Round 복제",
        description = "Round 설정, 지원 Form 전체 구조와 활성 공통 질문을 대상 Season의 새 DRAFT Round로 복제합니다."
    )
    public RecruitingIdResponse cloneRound(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long seasonId,
        @PathVariable @Positive Long roundId,
        @Valid @RequestBody CloneRecruitingRoundRequest request
    ) {
        return RecruitingIdResponse.from(cloneRoundUseCase.cloneRound(
            request.toCommand(seasonId, roundId, memberPrincipal.getMemberId())
        ));
    }

    @DeleteMapping("/seasons/{seasonId}/rounds/{roundId}")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    @Operation(
        operationId = "RECRUITING-ADMIN-017",
        summary = "모집 Round 삭제",
        description = "지원서와 Form 응답이 없는 DRAFT Round와 소유한 Form 구조를 완전히 삭제합니다."
    )
    public void deleteRound(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long seasonId,
        @PathVariable @Positive Long roundId
    ) {
        deleteRoundUseCase.deleteRound(DeleteRecruitingRoundCommand.builder()
            .seasonId(seasonId)
            .roundId(roundId)
            .requesterMemberId(memberPrincipal.getMemberId())
            .build());
    }
}
