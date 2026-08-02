package com.umc.product.recruiting.adapter.in.graphql;

import java.time.LocalDate;
import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingIdGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewScheduleBoardGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewScheduleGraphQlRequest.Confirm;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewScheduleGraphQlRequest.RequestAvailability;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewScheduleGraphQlRequest.Submit;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewScheduleGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewSessionGraphQlRequest.ConfirmSchedules;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewSessionGraphQlRequest.Session;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewSessionGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.SkipRecruitingInterviewGraphQlRequest;
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingInterviewSchedulesUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewSessionUseCase;
import com.umc.product.recruiting.application.port.in.command.SkipRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.DeleteRecruitingInterviewSessionCommand;
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
        @Argument Long applicationId
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return getInterviewScheduleUseCase.findByApplicationId(applicationId, requesterMemberId)
            .map(RecruitingInterviewScheduleGraphQlResponse::from)
            .orElse(null);
    }

    @QueryMapping
    public RecruitingInterviewSessionGraphQlResponse recruitingInterviewSession(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long roundId,
        @Argument Long sessionId
    ) {
        Long requesterMemberId = requireRoundEditPermission(memberPrincipal, seasonId, roundId);
        return RecruitingInterviewSessionGraphQlResponse.from(
            getInterviewSessionUseCase.getSession(roundId, sessionId, requesterMemberId)
        );
    }

    @QueryMapping
    public List<RecruitingInterviewSessionGraphQlResponse> recruitingInterviewSessions(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long roundId
    ) {
        Long requesterMemberId = requireRoundEditPermission(memberPrincipal, seasonId, roundId);
        return getInterviewSessionUseCase.listSessions(roundId, requesterMemberId).stream()
            .map(RecruitingInterviewSessionGraphQlResponse::from)
            .toList();
    }

    @QueryMapping
    public RecruitingInterviewScheduleBoardGraphQlResponse recruitingInterviewScheduleBoard(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long roundId,
        @Argument String date
    ) {
        Long requesterMemberId = requireRoundEditPermission(memberPrincipal, seasonId, roundId);
        return RecruitingInterviewScheduleBoardGraphQlResponse.from(
            getInterviewScheduleBoardUseCase.getBoard(roundId, LocalDate.parse(date), requesterMemberId)
        );
    }

    @MutationMapping
    public Boolean skipRecruitingInterview(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long applicationId,
        @Argument SkipRecruitingInterviewGraphQlRequest input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        permissionSupport.assertResourceBelongsToSeason(
            getApplicationQueryUseCase.isApplicationBelongsToSeason(applicationId, seasonId)
        );
        permissionSupport.assertRecruitmentPermission(requesterMemberId, seasonId, PermissionType.EDIT);
        SkipRecruitingInterviewGraphQlRequest actualInput = input == null
            ? new SkipRecruitingInterviewGraphQlRequest(null)
            : input;
        skipInterviewUseCase.skip(actualInput.toCommand(applicationId, requesterMemberId));
        return true;
    }

    @MutationMapping
    public RecruitingIdGraphQlResponse requestRecruitingInterviewAvailability(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId,
        @Argument RequestAvailability input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        return RecruitingIdGraphQlResponse.from(
            manageInterviewScheduleUseCase.requestAvailability(input.toCommand(applicationId, requesterMemberId))
        );
    }

    @MutationMapping
    public Boolean submitRecruitingInterviewAvailability(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId,
        @Argument @Valid Submit input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        manageInterviewScheduleUseCase.submitAvailability(input.toCommand(applicationId, requesterMemberId));
        return true;
    }

    @MutationMapping
    public Boolean confirmRecruitingInterviewSchedule(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long applicationId,
        @Argument Confirm input
    ) {
        Long requesterMemberId = permissionSupport.currentMemberId(memberPrincipal);
        manageInterviewScheduleUseCase.confirm(input.toCommand(applicationId, requesterMemberId));
        return true;
    }

    @MutationMapping
    public RecruitingIdGraphQlResponse createRecruitingInterviewSession(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long roundId,
        @Argument Session input
    ) {
        Long requesterMemberId = requireRoundEditPermission(memberPrincipal, seasonId, roundId);
        return RecruitingIdGraphQlResponse.from(
            manageInterviewSessionUseCase.createSession(input.toCreateCommand(roundId, requesterMemberId))
        );
    }

    @MutationMapping
    public Boolean updateRecruitingInterviewSession(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long roundId,
        @Argument Long sessionId,
        @Argument Session input
    ) {
        Long requesterMemberId = requireRoundEditPermission(memberPrincipal, seasonId, roundId);
        manageInterviewSessionUseCase.updateSession(input.toUpdateCommand(sessionId, roundId, requesterMemberId));
        return true;
    }

    @MutationMapping
    public Boolean deleteRecruitingInterviewSession(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long roundId,
        @Argument Long sessionId
    ) {
        Long requesterMemberId = requireRoundEditPermission(memberPrincipal, seasonId, roundId);
        manageInterviewSessionUseCase.deleteSession(
            DeleteRecruitingInterviewSessionCommand.of(sessionId, roundId, requesterMemberId)
        );
        return true;
    }

    @MutationMapping
    public Boolean confirmRecruitingInterviewSchedules(
        @Nullable @CurrentMember MemberPrincipal memberPrincipal,
        @Argument Long seasonId,
        @Argument Long roundId,
        @Argument @Valid ConfirmSchedules input
    ) {
        Long requesterMemberId = requireRoundEditPermission(memberPrincipal, seasonId, roundId);
        confirmInterviewSchedulesUseCase.confirmAll(input.toCommand(roundId, requesterMemberId));
        return true;
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
