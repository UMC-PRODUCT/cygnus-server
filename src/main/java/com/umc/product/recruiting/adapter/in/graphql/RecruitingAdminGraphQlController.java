package com.umc.product.recruiting.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.global.graphql.relay.ConnectionArguments;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.OffsetPageRequest;
import com.umc.product.global.graphql.relay.RelayConnection;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.CloneRecruitingRoundGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.CreateRecruitingRoundGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.CreateRecruitingSeasonGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.DeleteRecruitingRoundGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.GraphQlSuccessPayload;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingDecisionHistoryGraphQlConnection;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingDecisionHistorySearchGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingEvaluationStatisticsGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingEvaluationStatisticsGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingRoundIdGraphQlPayload;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingRoundSearchGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingSeasonGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingSeasonIdGraphQlPayload;
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
import com.umc.product.recruiting.application.port.in.query.CheckRecruitingRoundTitleUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingEvaluationStatisticsUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingSeasonConfigurationUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingDecisionHistoryUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingRoundGroupUseCase;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingAdminGraphQlController {

    private final GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    private final SearchRecruitingDecisionHistoryUseCase searchDecisionHistoryUseCase;
    private final GetRecruitingEvaluationStatisticsUseCase getEvaluationStatisticsUseCase;
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
    public RelayConnection<RecruitingSeasonSummaryGraphQlResponse> recruitingRoundGroups(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument RecruitingRoundSearchGraphQlRequest input,
        @Argument Integer first,
        @Argument String after,
        @Argument Integer last,
        @Argument String before
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        ConnectionArguments arguments = ConnectionArguments.of(first, after, last, before);
        return RelayConnection.fromList(
            searchRoundGroupUseCase.searchRoundGroups(input.toQuery(requesterMemberId)),
            arguments,
            RecruitingSeasonSummaryGraphQlResponse::from
        );
    }

    @QueryMapping
    public Boolean recruitingRoundTitleAvailable(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument String seasonId,
        @Argument String title,
        @Argument String excludedRoundId
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        Long decodedSeasonId = GlobalId.decodeLong(seasonId, GlobalIdTypes.RECRUITING_SEASON);
        permissionSupport.assertRecruitmentPermission(requesterMemberId, decodedSeasonId, PermissionType.READ);
        return checkRoundTitleUseCase.isTitleAvailable(
            decodedSeasonId,
            title,
            excludedRoundId == null ? null : GlobalId.decodeLong(excludedRoundId, GlobalIdTypes.RECRUITING_ROUND)
        );
    }

    @QueryMapping
    public RecruitingSeasonGraphQlResponse recruitingSeason(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument String id
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        Long seasonId = GlobalId.decodeLong(id, GlobalIdTypes.RECRUITING_SEASON);
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.READ);
        return RecruitingSeasonGraphQlResponse.from(
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
            getApplicationQueryUseCase.getStatusSummary(input.toQuery(requesterMemberId))
        );
    }

    @QueryMapping
    public RecruitingEvaluationStatisticsGraphQlResponse recruitingEvaluationStatistics(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument RecruitingEvaluationStatisticsGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingEvaluationStatisticsGraphQlResponse.from(
            getEvaluationStatisticsUseCase.getEvaluationStatistics(input.toQuery(requesterMemberId))
        );
    }

    @QueryMapping
    public RecruitingDecisionHistoryGraphQlConnection recruitingDecisionHistories(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument RecruitingDecisionHistorySearchGraphQlRequest input,
        @Argument Integer first,
        @Argument String after,
        @Argument Integer last,
        @Argument String before
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        ConnectionArguments arguments = ConnectionArguments.of(first, after, last, before);
        var pageable = arguments.toPageable(() -> searchDecisionHistoryUseCase.search(
            input.toQuery(requesterMemberId, new OffsetPageRequest(0, 1))
        ).page().getTotalElements());
        return RecruitingDecisionHistoryGraphQlConnection.from(
            searchDecisionHistoryUseCase.search(input.toQuery(requesterMemberId, pageable)),
            arguments
        );
    }

    @MutationMapping
    public RecruitingSeasonIdGraphQlPayload createRecruitingSeason(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument CreateRecruitingSeasonGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertRecruitmentTypePermission(requesterMemberId, PermissionType.WRITE);
        return RecruitingSeasonIdGraphQlPayload.of(
            createSeasonUseCase.createSeason(input.toCommand(requesterMemberId))
        );
    }

    @MutationMapping
    public GraphQlSuccessPayload updateRecruitingSeason(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument UpdateRecruitingSeasonGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertRecruitmentPermission(requesterMemberId, input.decodedSeasonId(), PermissionType.EDIT);
        updateSeasonUseCase.updateSeason(input.toCommand());
        return GraphQlSuccessPayload.ok();
    }

    @MutationMapping
    public GraphQlSuccessPayload replaceRecruitingSeasonTrackQuotas(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument ReplaceRecruitingSeasonTrackQuotasGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertRecruitmentPermission(requesterMemberId, input.decodedSeasonId(), PermissionType.EDIT);
        replaceSeasonTrackQuotasUseCase.replaceQuotas(input.toCommand());
        return GraphQlSuccessPayload.ok();
    }

    @MutationMapping
    public RecruitingRoundIdGraphQlPayload createRecruitingRound(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument CreateRecruitingRoundGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertRecruitmentPermission(requesterMemberId, input.decodedSeasonId(), PermissionType.WRITE);
        return RecruitingRoundIdGraphQlPayload.of(createRoundUseCase.createRound(input.toCommand()));
    }

    @MutationMapping
    public GraphQlSuccessPayload updateRecruitingRoundStatus(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument UpdateRecruitingRoundStatusGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        requireRoundPermission(requesterMemberId, input.decodedSeasonId(), input.decodedRoundId());
        updateRoundStatusUseCase.updateRoundStatus(input.toCommand(requesterMemberId));
        return GraphQlSuccessPayload.ok();
    }

    @MutationMapping
    public GraphQlSuccessPayload updateRecruitingRound(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument UpdateRecruitingRoundGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        requireRoundPermission(requesterMemberId, input.decodedSeasonId(), input.decodedRoundId());
        updateRoundUseCase.updateRound(input.toCommand(requesterMemberId));
        return GraphQlSuccessPayload.ok();
    }

    @MutationMapping
    public RecruitingRoundIdGraphQlPayload cloneRecruitingRound(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument CloneRecruitingRoundGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingRoundIdGraphQlPayload.of(
            cloneRoundUseCase.cloneRound(input.toCommand(requesterMemberId))
        );
    }

    @MutationMapping
    public GraphQlSuccessPayload deleteRecruitingRound(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument DeleteRecruitingRoundGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        deleteRoundUseCase.deleteRound(input.toCommand(requesterMemberId));
        return GraphQlSuccessPayload.ok();
    }

    private void requireRoundPermission(Long requesterMemberId, Long seasonId, Long roundId) {
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isRoundBelongsToSeason(roundId, seasonId)
        );
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.EDIT);
    }
}
