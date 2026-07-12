package com.umc.product.recruiting.adapter.in.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingIdGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewScheduleGraphQlRequest.Confirm;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewScheduleGraphQlRequest.RequestAvailability;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewScheduleGraphQlRequest.SubmitAvailability;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingInterviewScheduleGraphQlResponse;
import com.umc.product.recruiting.adapter.in.graphql.dto.SkipRecruitingInterviewGraphQlRequest;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingInterviewScheduleUseCase;
import com.umc.product.recruiting.application.port.in.command.SkipRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewScheduleUseCase;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecruitingScheduleGraphQlController {

    private final GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    private final GetRecruitingInterviewScheduleUseCase getInterviewScheduleUseCase;
    private final ManageRecruitingInterviewScheduleUseCase manageInterviewScheduleUseCase;
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
        @Argument SubmitAvailability input
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
}
