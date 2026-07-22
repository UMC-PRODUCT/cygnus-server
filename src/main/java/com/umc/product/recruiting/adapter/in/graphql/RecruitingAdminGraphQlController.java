package com.umc.product.recruiting.adapter.in.graphql;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.organization.adapter.in.graphql.dto.GisuGraphQlResponse;
import com.umc.product.organization.adapter.in.graphql.dto.SchoolGraphQlResponse;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.recruiting.adapter.in.graphql.dto.CloneRecruitingRoundGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.CreateRecruitingRoundGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.CreateRecruitingSeasonGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingDeletedGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingPublicRoundSearchGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingRoundGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingRoundManagementGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingSeasonGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingSeasonManagementGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingSeasonManagementSummaryGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingSeasonSearchGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingStatusSummaryGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.UpdateRecruitingRoundGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.UpdateRecruitingRoundStatusGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.UpdateRecruitingSeasonGraphQlRequest;
import com.umc.product.recruiting.application.port.in.command.CloneRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.command.DeleteRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.DeleteRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.query.CheckRecruitingRoundTitleUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingResourceUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingSeasonConfigurationUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchPublicRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingSeasonUseCase;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingAdminGraphQlController {

    private final SearchRecruitingSeasonUseCase searchSeasonUseCase;
    private final SearchPublicRecruitingRoundUseCase searchPublicRoundUseCase;
    private final GetRecruitingSeasonConfigurationUseCase getSeasonConfigurationUseCase;
    private final GetRecruitingResourceUseCase getResourceUseCase;
    private final GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    private final CheckRecruitingRoundTitleUseCase checkRoundTitleUseCase;
    private final CreateRecruitingSeasonUseCase createSeasonUseCase;
    private final UpdateRecruitingSeasonUseCase updateSeasonUseCase;
    private final CreateRecruitingRoundUseCase createRoundUseCase;
    private final UpdateRecruitingRoundStatusUseCase updateRoundStatusUseCase;
    private final UpdateRecruitingRoundUseCase updateRoundUseCase;
    private final CloneRecruitingRoundUseCase cloneRoundUseCase;
    private final DeleteRecruitingRoundUseCase deleteRoundUseCase;
    private final GetGisuUseCase getGisuUseCase;
    private final GetSchoolUseCase getSchoolUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @QueryMapping
    public List<RecruitingSeasonGraphQlResponse> recruitingSeasons(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument("filter") RecruitingSeasonSearchGraphQlRequest filter
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return searchSeasonUseCase.searchSeasons(filter.toQuery(requesterMemberId)).stream()
            .map(RecruitingSeasonGraphQlResponse::from)
            .toList();
    }

    @QueryMapping
    public RecruitingSeasonGraphQlResponse recruitingSeason(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long id
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertRecruitmentPermission(requesterMemberId, id, PermissionType.READ);
        return RecruitingSeasonGraphQlResponse.from(getSeasonConfigurationUseCase.getBySeasonId(id));
    }

    @QueryMapping
    public List<RecruitingRoundGraphQlResponse> recruitingRounds(
        @Argument("filter") RecruitingPublicRoundSearchGraphQlRequest filter
    ) {
        return searchPublicRoundUseCase.searchPublicRounds(filter.toQuery()).stream()
            .flatMap(group -> group.rounds().stream().map(round -> RecruitingRoundGraphQlResponse.from(round, group)))
            .toList();
    }

    @QueryMapping
    public RecruitingRoundGraphQlResponse recruitingRound(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long id
    ) {
        return RecruitingRoundGraphQlResponse.from(
            getResourceUseCase.getRound(id, permissionSupport.nullableCurrentMemberId(memberPrincipal))
        );
    }

    @MutationMapping
    public RecruitingSeasonGraphQlResponse createRecruitingSeason(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument CreateRecruitingSeasonGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertRecruitmentTypePermission(requesterMemberId, PermissionType.WRITE);
        Long seasonId = createSeasonUseCase.createSeason(input.toCommand(requesterMemberId));
        return RecruitingSeasonGraphQlResponse.from(getSeasonConfigurationUseCase.getBySeasonId(seasonId));
    }

    @MutationMapping
    public RecruitingSeasonGraphQlResponse updateRecruitingSeason(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long id,
        @Argument UpdateRecruitingSeasonGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertRecruitmentPermission(requesterMemberId, id, PermissionType.EDIT);
        updateSeasonUseCase.updateSeasonAndQuotas(
            input.toCommand(id),
            input.quotaReplacement().toCommand(id)
        );
        return RecruitingSeasonGraphQlResponse.from(getSeasonConfigurationUseCase.getBySeasonId(id));
    }

    @MutationMapping
    public RecruitingRoundGraphQlResponse createRecruitingRound(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument CreateRecruitingRoundGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.WRITE);
        Long roundId = createRoundUseCase.createRound(input.toCommand(seasonId));
        return round(roundId, requesterMemberId);
    }

    @MutationMapping
    public RecruitingRoundGraphQlResponse updateRecruitingRound(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long id,
        @Argument UpdateRecruitingRoundGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        RecruitingRoundGraphQlResponse current = round(id, requesterMemberId);
        permissionSupport.assertRecruitmentPermission(requesterMemberId, current.seasonId(), PermissionType.EDIT);
        updateRoundUseCase.updateRound(input.toCommand(current.seasonId(), id, requesterMemberId));
        return round(id, requesterMemberId);
    }

    @MutationMapping
    public RecruitingRoundGraphQlResponse changeRecruitingRoundStatus(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long id,
        @Argument RecruitingRoundStatus status
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        RecruitingRoundGraphQlResponse current = round(id, requesterMemberId);
        permissionSupport.assertRecruitmentPermission(requesterMemberId, current.seasonId(), PermissionType.EDIT);
        UpdateRecruitingRoundStatusGraphQlRequest input = new UpdateRecruitingRoundStatusGraphQlRequest(status);
        updateRoundStatusUseCase.updateRoundStatus(input.toCommand(current.seasonId(), id, requesterMemberId));
        return round(id, requesterMemberId);
    }

    @MutationMapping
    public RecruitingRoundGraphQlResponse cloneRecruitingRound(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long id,
        @Argument CloneRecruitingRoundGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        RecruitingRoundGraphQlResponse source = round(id, requesterMemberId);
        Long clonedId = cloneRoundUseCase.cloneRound(
            input.toCommand(source.seasonId(), id, requesterMemberId)
        );
        return round(clonedId, requesterMemberId);
    }

    @MutationMapping
    public RecruitingDeletedGraphQlResponse deleteRecruitingRound(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long id
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        RecruitingRoundGraphQlResponse current = round(id, requesterMemberId);
        deleteRoundUseCase.deleteRound(DeleteRecruitingRoundCommand.builder()
            .seasonId(current.seasonId())
            .roundId(id)
            .requesterMemberId(requesterMemberId)
            .build());
        return new RecruitingDeletedGraphQlResponse(id);
    }

    @BatchMapping(typeName = "RecruitingSeason", field = "gisu")
    public Map<RecruitingSeasonGraphQlResponse, GisuGraphQlResponse> gisus(
        List<RecruitingSeasonGraphQlResponse> seasons
    ) {
        Set<Long> ids = seasons.stream().map(RecruitingSeasonGraphQlResponse::gisuId).collect(Collectors.toSet());
        Map<Long, GisuGraphQlResponse> byId = getGisuUseCase.getByIds(ids).stream()
            .map(GisuGraphQlResponse::from)
            .collect(Collectors.toMap(GisuGraphQlResponse::id, Function.identity()));
        return seasons.stream().collect(Collectors.toMap(
            Function.identity(),
            season -> byId.get(season.gisuId()),
            (left, right) -> left,
            LinkedHashMap::new
        ));
    }

    @BatchMapping(typeName = "RecruitingSeason", field = "school")
    public Map<RecruitingSeasonGraphQlResponse, SchoolGraphQlResponse> schools(
        List<RecruitingSeasonGraphQlResponse> seasons
    ) {
        Set<Long> ids = seasons.stream().map(RecruitingSeasonGraphQlResponse::schoolId).collect(Collectors.toSet());
        Map<Long, SchoolGraphQlResponse> byId = getSchoolUseCase.listDetailsByIds(ids).stream()
            .map(SchoolGraphQlResponse::from)
            .collect(Collectors.toMap(SchoolGraphQlResponse::id, Function.identity()));
        return seasons.stream().collect(Collectors.toMap(
            Function.identity(),
            season -> byId.get(season.schoolId()),
            (left, right) -> left,
            LinkedHashMap::new
        ));
    }

    @BatchMapping(typeName = "RecruitingRound", field = "season")
    public Map<RecruitingRoundGraphQlResponse, RecruitingSeasonGraphQlResponse> seasons(
        List<RecruitingRoundGraphQlResponse> rounds
    ) {
        Map<Long, RecruitingSeasonGraphQlResponse> byId = rounds.stream()
            .map(RecruitingRoundGraphQlResponse::seasonId)
            .distinct()
            .collect(Collectors.toMap(
                Function.identity(),
                id -> RecruitingSeasonGraphQlResponse.from(getSeasonConfigurationUseCase.getBySeasonId(id))
            ));
        return rounds.stream().collect(Collectors.toMap(
            Function.identity(),
            round -> byId.get(round.seasonId()),
            (left, right) -> left,
            LinkedHashMap::new
        ));
    }

    @SchemaMapping(typeName = "RecruitingSeason", field = "management")
    public RecruitingSeasonManagementGraphQlResponse seasonManagement(RecruitingSeasonGraphQlResponse season) {
        Long requesterMemberId = permissionSupport.nullableCurrentMemberId();
        if (requesterMemberId == null || !permissionSupport.hasRecruitmentPermission(
            requesterMemberId,
            season.id(),
            PermissionType.READ
        )) {
            return null;
        }
        RecruitingSeasonGraphQlResponse complete = season.quotas().isEmpty()
            ? RecruitingSeasonGraphQlResponse.from(getSeasonConfigurationUseCase.getBySeasonId(season.id()))
            : season;
        return RecruitingSeasonManagementGraphQlResponse.from(complete);
    }

    @SchemaMapping(typeName = "RecruitingRound", field = "management")
    public RecruitingRoundManagementGraphQlResponse roundManagement(RecruitingRoundGraphQlResponse round) {
        Long requesterMemberId = permissionSupport.nullableCurrentMemberId();
        if (requesterMemberId == null || !permissionSupport.hasRecruitmentPermission(
            requesterMemberId,
            round.seasonId(),
            PermissionType.READ
        )) {
            return null;
        }
        return RecruitingRoundManagementGraphQlResponse.from(round);
    }

    @SchemaMapping(typeName = "RecruitingSeasonManagement", field = "roundTitleAvailable")
    public Boolean roundTitleAvailable(
        RecruitingSeasonManagementGraphQlResponse management,
        @Argument String title,
        @Argument Long excludedRoundId
    ) {
        return checkRoundTitleUseCase.isTitleAvailable(management.seasonId(), title, excludedRoundId);
    }

    @SchemaMapping(typeName = "RecruitingSeasonManagement", field = "statusSummary")
    public RecruitingStatusSummaryGraphQlResponse statusSummary(
        RecruitingSeasonManagementGraphQlResponse management,
        @Argument("filter") RecruitingSeasonManagementSummaryGraphQlRequest filter
    ) {
        RecruitingSeasonManagementSummaryGraphQlRequest actualFilter = filter == null
            ? new RecruitingSeasonManagementSummaryGraphQlRequest(null, null, null)
            : filter;
        return RecruitingStatusSummaryGraphQlResponse.from(getApplicationQueryUseCase.getStatusSummary(
            actualFilter.toQuery(management.gisuId(), permissionSupport.currentMemberId())
        ));
    }

    @BatchMapping(typeName = "RecruitingSchoolStatusSummary", field = "school")
    public Map<RecruitingStatusSummaryGraphQlResponse.SchoolSummary, SchoolGraphQlResponse> summarySchools(
        List<RecruitingStatusSummaryGraphQlResponse.SchoolSummary> summaries
    ) {
        Set<Long> ids = summaries.stream()
            .map(RecruitingStatusSummaryGraphQlResponse.SchoolSummary::schoolId)
            .collect(Collectors.toSet());
        Map<Long, SchoolGraphQlResponse> byId = getSchoolUseCase.listDetailsByIds(ids).stream()
            .map(SchoolGraphQlResponse::from)
            .collect(Collectors.toMap(SchoolGraphQlResponse::id, Function.identity()));
        return summaries.stream().collect(Collectors.toMap(
            Function.identity(),
            summary -> byId.get(summary.schoolId()),
            (left, right) -> left,
            LinkedHashMap::new
        ));
    }

    private RecruitingRoundGraphQlResponse round(Long roundId, Long requesterMemberId) {
        return RecruitingRoundGraphQlResponse.from(getResourceUseCase.getRound(roundId, requesterMemberId));
    }
}
