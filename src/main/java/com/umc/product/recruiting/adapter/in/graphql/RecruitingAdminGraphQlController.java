package com.umc.product.recruiting.adapter.in.graphql;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.CloneRecruitingRoundGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.CreateRecruitingRoundGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.CreateRecruitingSeasonGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingIdGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingRoundSearchGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingSeasonConfigurationGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingSeasonSummaryGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingStatusSummaryGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingStatusSummaryGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.ReplaceRecruitingSeasonTrackQuotasGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.UpdateRecruitingRoundGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.UpdateRecruitingRoundStatusGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.UpdateRecruitingSeasonGraphQlRequest;
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
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingSeasonConfigurationUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingRoundGroupUseCase;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingAdminGraphQlController {

    private final GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    private final GetRecruitingSeasonConfigurationUseCase getSeasonConfigurationUseCase;
    private final SearchRecruitingRoundGroupUseCase searchRoundGroupUseCase;
    private final CheckRecruitingRoundTitleUseCase checkRoundTitleUseCase;
    private final CreateRecruitingSeasonUseCase createSeasonUseCase;
    private final UpdateRecruitingSeasonUseCase updateSeasonUseCase;
    private final ReplaceRecruitingSeasonTrackQuotasUseCase replaceSeasonTrackQuotasUseCase;
    private final CreateRecruitingRoundUseCase createRoundUseCase;
    private final UpdateRecruitingRoundStatusUseCase updateRoundStatusUseCase;
    private final UpdateRecruitingRoundUseCase updateRoundUseCase;
    private final CloneRecruitingRoundUseCase cloneRoundUseCase;
    private final DeleteRecruitingRoundUseCase deleteRoundUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @QueryMapping
    public List<RecruitingSeasonSummaryGraphQlResponse> recruitingRoundGroups(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument RecruitingRoundSearchGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return searchRoundGroupUseCase.searchRoundGroups(input.toQuery(requesterMemberId)).stream()
            .map(RecruitingSeasonSummaryGraphQlResponse::from)
            .toList();
    }

    @QueryMapping
    public Boolean recruitingRoundTitleAvailable(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument String title,
        @Argument Long excludedRoundId
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.READ);
        return checkRoundTitleUseCase.isTitleAvailable(seasonId, title, excludedRoundId);
    }

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
        return RecruitingStatusSummaryGraphQlResponse.from(
            getApplicationQueryUseCase.getStatusSummary(
                input.gisuId(),
                input.schoolId(),
                input.roundId(),
                requesterMemberId
            )
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
    public Boolean updateRecruitingSeason(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument UpdateRecruitingSeasonGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.EDIT);
        updateSeasonUseCase.updateSeason(input.toCommand(seasonId));
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
        updateRoundStatusUseCase.updateRoundStatus(input.toCommand(seasonId, roundId, requesterMemberId));
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
        updateRoundUseCase.updateRound(input.toCommand(seasonId, roundId, requesterMemberId));
        return true;
    }

    @MutationMapping
    public RecruitingIdGraphQlResponse cloneRecruitingRound(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long roundId,
        @Argument CloneRecruitingRoundGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingIdGraphQlResponse.from(
            cloneRoundUseCase.cloneRound(input.toCommand(seasonId, roundId, requesterMemberId))
        );
    }

    @MutationMapping
    public Boolean deleteRecruitingRound(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long roundId
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        deleteRoundUseCase.deleteRound(DeleteRecruitingRoundCommand.builder()
            .seasonId(seasonId)
            .roundId(roundId)
            .requesterMemberId(requesterMemberId)
            .build());
        return true;
    }

    private void requireRoundPermission(Long requesterMemberId, Long seasonId, Long roundId) {
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isRoundBelongsToSeason(roundId, seasonId)
        );
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.EDIT);
    }
}
