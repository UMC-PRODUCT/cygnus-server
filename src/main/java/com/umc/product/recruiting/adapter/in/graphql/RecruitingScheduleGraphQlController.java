package com.umc.product.recruiting.adapter.in.graphql;

import java.time.LocalDate;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.global.graphql.relay.ConnectionArguments;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.RelayConnection;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.GraphQlSuccessPayload;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewScheduleBoardGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewScheduleGraphQlRequest.Confirm;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewScheduleGraphQlRequest.RequestAvailability;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewScheduleGraphQlRequest.Submit;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewScheduleGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewScheduleIdGraphQlPayload;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewSessionGraphQlRequest.ConfirmSchedules;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewSessionGraphQlRequest.Create;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewSessionGraphQlRequest.Delete;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewSessionGraphQlRequest.Update;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewSessionGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewSessionIdGraphQlPayload;
import com.umc.product.recruiting.adapter.in.graphql.dto.SkipRecruitingInterviewGraphQlRequest;
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingInterviewSchedulesUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewSessionUseCase;
import com.umc.product.recruiting.application.port.in.command.SkipRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewScheduleBoardUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewSessionUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingScheduleGraphQlController {

    private final GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    private final GetRecruitingInterviewScheduleUseCase getInterviewScheduleUseCase;
    private final ManageRecruitingInterviewScheduleUseCase manageInterviewScheduleUseCase;
    private final ManageRecruitingInterviewSessionUseCase manageInterviewSessionUseCase;
    private final GetRecruitingInterviewSessionUseCase getInterviewSessionUseCase;
    private final GetRecruitingInterviewScheduleBoardUseCase getInterviewScheduleBoardUseCase;
    private final ConfirmRecruitingInterviewSchedulesUseCase confirmInterviewSchedulesUseCase;
    private final SkipRecruitingInterviewUseCase skipInterviewUseCase;
    private final RecruitingGraphQlPermissionSupport permissionSupport;

    @QueryMapping
    public RecruitingInterviewScheduleGraphQlResponse recruitingInterviewSchedule(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument String applicationId
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return getInterviewScheduleUseCase.findByApplicationId(
                GlobalId.decodeLong(applicationId, GlobalIdTypes.RECRUITING_APPLICATION),
                requesterMemberId
            )
            .map(RecruitingInterviewScheduleGraphQlResponse::from)
            .orElse(null);
    }

    @QueryMapping
    public RecruitingInterviewSessionGraphQlResponse recruitingInterviewSession(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument String seasonId,
        @Argument String roundId,
        @Argument String sessionId
    ) {
        Long decodedRoundId = GlobalId.decodeLong(roundId, GlobalIdTypes.RECRUITING_ROUND);
        Long requesterMemberId = requireRoundEditPermission(
            memberPrincipal,
            GlobalId.decodeLong(seasonId, GlobalIdTypes.RECRUITING_SEASON),
            decodedRoundId
        );
        return RecruitingInterviewSessionGraphQlResponse.from(
            getInterviewSessionUseCase.getSession(
                decodedRoundId,
                GlobalId.decodeLong(sessionId, GlobalIdTypes.RECRUITING_INTERVIEW_SESSION),
                requesterMemberId
            )
        );
    }

    @QueryMapping
    public RelayConnection<RecruitingInterviewSessionGraphQlResponse> recruitingInterviewSessions(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument String seasonId,
        @Argument String roundId,
        @Argument Integer first,
        @Argument String after,
        @Argument Integer last,
        @Argument String before
    ) {
        Long decodedRoundId = GlobalId.decodeLong(roundId, GlobalIdTypes.RECRUITING_ROUND);
        Long requesterMemberId = requireRoundEditPermission(
            memberPrincipal,
            GlobalId.decodeLong(seasonId, GlobalIdTypes.RECRUITING_SEASON),
            decodedRoundId
        );
        ConnectionArguments arguments = ConnectionArguments.of(first, after, last, before);
        return RelayConnection.fromList(
            getInterviewSessionUseCase.listSessions(decodedRoundId, requesterMemberId),
            arguments,
            RecruitingInterviewSessionGraphQlResponse::from
        );
    }

    @QueryMapping
    public RecruitingInterviewScheduleBoardGraphQlResponse recruitingInterviewScheduleBoard(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument String seasonId,
        @Argument String roundId,
        @Argument String date
    ) {
        Long decodedRoundId = GlobalId.decodeLong(roundId, GlobalIdTypes.RECRUITING_ROUND);
        Long requesterMemberId = requireRoundEditPermission(
            memberPrincipal,
            GlobalId.decodeLong(seasonId, GlobalIdTypes.RECRUITING_SEASON),
            decodedRoundId
        );
        return RecruitingInterviewScheduleBoardGraphQlResponse.from(
            getInterviewScheduleBoardUseCase.getBoard(decodedRoundId, LocalDate.parse(date), requesterMemberId)
        );
    }

    @MutationMapping
    public GraphQlSuccessPayload skipRecruitingInterview(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument SkipRecruitingInterviewGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        Long seasonId = input.decodedSeasonId();
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isApplicationBelongsToSeason(input.decodedApplicationId(), seasonId)
        );
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.EDIT);
        skipInterviewUseCase.skip(input.toCommand(requesterMemberId));
        return GraphQlSuccessPayload.ok();
    }

    @MutationMapping
    public RecruitingInterviewScheduleIdGraphQlPayload requestRecruitingInterviewAvailability(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument RequestAvailability input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingInterviewScheduleIdGraphQlPayload.of(
            manageInterviewScheduleUseCase.requestAvailability(input.toCommand(requesterMemberId))
        );
    }

    @MutationMapping
    public GraphQlSuccessPayload submitRecruitingInterviewAvailability(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument @Valid Submit input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        manageInterviewScheduleUseCase.submitAvailability(input.toCommand(requesterMemberId));
        return GraphQlSuccessPayload.ok();
    }

    @MutationMapping
    public GraphQlSuccessPayload confirmRecruitingInterviewSchedule(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Confirm input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        manageInterviewScheduleUseCase.confirm(input.toCommand(requesterMemberId));
        return GraphQlSuccessPayload.ok();
    }

    @MutationMapping
    public RecruitingInterviewSessionIdGraphQlPayload createRecruitingInterviewSession(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument @Valid Create input
    ) {
        Long requesterMemberId = requireRoundEditPermission(
            memberPrincipal,
            input.decodedSeasonId(),
            input.decodedRoundId()
        );
        return RecruitingInterviewSessionIdGraphQlPayload.of(
            manageInterviewSessionUseCase.createSession(input.toCommand(requesterMemberId))
        );
    }

    @MutationMapping
    public GraphQlSuccessPayload updateRecruitingInterviewSession(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument @Valid Update input
    ) {
        Long requesterMemberId = requireRoundEditPermission(
            memberPrincipal,
            input.decodedSeasonId(),
            input.decodedRoundId()
        );
        manageInterviewSessionUseCase.updateSession(input.toCommand(requesterMemberId));
        return GraphQlSuccessPayload.ok();
    }

    @MutationMapping
    public GraphQlSuccessPayload deleteRecruitingInterviewSession(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Delete input
    ) {
        Long requesterMemberId = requireRoundEditPermission(
            memberPrincipal,
            input.decodedSeasonId(),
            input.decodedRoundId()
        );
        manageInterviewSessionUseCase.deleteSession(input.toCommand(requesterMemberId));
        return GraphQlSuccessPayload.ok();
    }

    @MutationMapping
    public GraphQlSuccessPayload confirmRecruitingInterviewSchedules(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument @Valid ConfirmSchedules input
    ) {
        Long requesterMemberId = requireRoundEditPermission(
            memberPrincipal,
            input.decodedSeasonId(),
            input.decodedRoundId()
        );
        confirmInterviewSchedulesUseCase.confirmAll(input.toCommand(requesterMemberId));
        return GraphQlSuccessPayload.ok();
    }

    private Long requireRoundEditPermission(MemberPrincipal memberPrincipal, Long seasonId, Long roundId) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isRoundBelongsToSeason(roundId, seasonId)
        );
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.EDIT);
        return requesterMemberId;
    }
}
