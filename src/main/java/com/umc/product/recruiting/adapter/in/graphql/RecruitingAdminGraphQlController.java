package com.umc.product.recruiting.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.CreateRecruitingRoundGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.CreateRecruitingSeasonGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingIdGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingSeasonConfigurationGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingStatusSummaryGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingStatusSummaryGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.ReplaceRecruitingSeasonTrackQuotasGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.UpdateRecruitingRoundGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.UpdateRecruitingRoundStatusGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.UpdateRecruitingSeasonStatusGraphQlRequest;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.command.ReplaceRecruitingSeasonTrackQuotasUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingSeasonStatusUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingSeasonConfigurationUseCase;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingAdminGraphQlController {

    private final GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    private final GetRecruitingSeasonConfigurationUseCase getSeasonConfigurationUseCase;
    private final CreateRecruitingSeasonUseCase createSeasonUseCase;
    private final UpdateRecruitingSeasonStatusUseCase updateSeasonStatusUseCase;
    private final ReplaceRecruitingSeasonTrackQuotasUseCase replaceSeasonTrackQuotasUseCase;
    private final CreateRecruitingRoundUseCase createRoundUseCase;
    private final UpdateRecruitingRoundStatusUseCase updateRoundStatusUseCase;
    private final UpdateRecruitingRoundUseCase updateRoundUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @QueryMapping
    public RecruitingSeasonConfigurationGraphQlResponse recruitingSeasonConfiguration(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.READ);
        return RecruitingSeasonConfigurationGraphQlResponse.from(
            getSeasonConfigurationUseCase.getBySeasonId(seasonId)
        );
    }

    @QueryMapping
    public RecruitingStatusSummaryGraphQlResponse recruitingStatusSummary(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument RecruitingStatusSummaryGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertRecruitmentTypePermission(requesterMemberId, PermissionType.MANAGE);
        return RecruitingStatusSummaryGraphQlResponse.from(
            getApplicationQueryUseCase.getStatusSummary(input.gisuId(), input.schoolId())
        );
    }

    @MutationMapping
    public RecruitingIdGraphQlResponse createRecruitingSeason(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument CreateRecruitingSeasonGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertRecruitmentTypePermission(requesterMemberId, PermissionType.WRITE);
        return RecruitingIdGraphQlResponse.from(
            createSeasonUseCase.createSeason(input.toCommand(requesterMemberId))
        );
    }

    @MutationMapping
    public Boolean updateRecruitingSeasonStatus(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument UpdateRecruitingSeasonStatusGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.EDIT);
        updateSeasonStatusUseCase.updateSeasonStatus(input.toCommand(seasonId));
        return true;
    }

    @MutationMapping
    public Boolean replaceRecruitingSeasonTrackQuotas(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument ReplaceRecruitingSeasonTrackQuotasGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.EDIT);
        replaceSeasonTrackQuotasUseCase.replaceQuotas(input.toCommand(seasonId));
        return true;
    }

    @MutationMapping
    public RecruitingIdGraphQlResponse createRecruitingRound(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument CreateRecruitingRoundGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.WRITE);
        return RecruitingIdGraphQlResponse.from(createRoundUseCase.createRound(input.toCommand(seasonId)));
    }

    @MutationMapping
    public Boolean updateRecruitingRoundStatus(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long roundId,
        @Argument UpdateRecruitingRoundStatusGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        requireRoundPermission(requesterMemberId, seasonId, roundId);
        updateRoundStatusUseCase.updateRoundStatus(input.toCommand(seasonId, roundId));
        return true;
    }

    @MutationMapping
    public Boolean updateRecruitingRound(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long roundId,
        @Argument UpdateRecruitingRoundGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        requireRoundPermission(requesterMemberId, seasonId, roundId);
        updateRoundUseCase.updateRound(input.toCommand(seasonId, roundId));
        return true;
    }

    private void requireRoundPermission(Long requesterMemberId, Long seasonId, Long roundId) {
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isRoundBelongsToSeason(roundId, seasonId)
        );
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.EDIT);
    }
}
